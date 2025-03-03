package com.stratocloud.account;

import com.stratocloud.account.cmd.*;
import com.stratocloud.account.query.DescribeAccountsRequest;
import com.stratocloud.account.query.NestedAccountResponse;
import com.stratocloud.account.response.*;
import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.job.*;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.ManageExternalResourceTaskInputs;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceFilters;
import com.stratocloud.resource.task.ManageExternalResourceTaskHandler;
import com.stratocloud.validate.ValidateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExternalAccountServiceImpl implements ExternalAccountService {

    private final ExternalAccountRepository repository;

    private final ResourceRepository resourceRepository;

    public ExternalAccountServiceImpl(ExternalAccountRepository repository,
                                      ResourceRepository resourceRepository) {
        this.repository = repository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateExternalAccountResponse createExternalAccount(CreateExternalAccountCmd cmd) {
        String providerId = cmd.getProviderId();
        String name = cmd.getName();
        Map<String, Object> properties = cmd.getProperties();
        String description = cmd.getDescription();

        ExternalAccount externalAccount = new ExternalAccount(providerId, name, properties, description);

        externalAccount.validateConnection();

        externalAccount.synchronizeState();

        externalAccount = repository.save(externalAccount);

        AuditLogContext.current().addAuditObject(
                new AuditObject(externalAccount.getId().toString(), externalAccount.getName())
        );

        return new CreateExternalAccountResponse(externalAccount.getId());
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateExternalAccountResponse updateExternalAccount(UpdateExternalAccountCmd cmd) {
        Long externalAccountId = cmd.getExternalAccountId();

        String name = cmd.getName();
        Map<String, Object> properties = cmd.getProperties();
        String description = cmd.getDescription();

        ExternalAccount account = repository.findExternalAccount(externalAccountId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(account.getId().toString(), account.getName())
        );

        account.update(name, properties, description);

        account.validateConnection();

        account.synchronizeState();

        repository.save(account);

        return new UpdateExternalAccountResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteExternalAccountsResponse deleteExternalAccounts(DeleteExternalAccountsCmd cmd) {
        List<Long> accountIds = cmd.getExternalAccountIds();

        ResourceFilters filters = ResourceFilters.builder().accountIds(accountIds).build();

        long count = resourceRepository.countByFilters(filters);

        if(count > 0)
            throw new BadCommandException("云账号下仍有资源存在");

        accountIds.forEach(this::deleteExternalAccount);

        return new DeleteExternalAccountsResponse();
    }

    private void deleteExternalAccount(Long accountId) {
        ExternalAccount account = repository.findExternalAccount(accountId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(account.getId().toString(), account.getName())
        );

        repository.delete(account);
    }


    @Override
    @Transactional
    @ValidateRequest
    public SynchronizeAccountsResponse synchronizeAccounts(SynchronizeAccountsCmd cmd) {
        List<Long> accountIds = cmd.getAccountIds();
        accountIds.forEach(this::synchronizeAccount);
        return new SynchronizeAccountsResponse();
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedAccountResponse> describeAccounts(DescribeAccountsRequest request) {
        ExternalAccountFilters filters = new ExternalAccountFilters(
                request.getAccountIds(),
                request.getProviderIds(),
                request.getResourceCategory(),
                request.getSearch(),
                request.getDisabled()
        );

        Page<ExternalAccount> page = repository.page(filters, request.getPageable());

        return page.map(this::toNestedAccountResponse);
    }

    @Override
    @Transactional
    @ValidateRequest
    public EnableAccountsResponse enableExternalAccounts(EnableAccountsCmd cmd) {
        List<Long> accountIds = cmd.getAccountIds();

        for (Long accountId : accountIds) {
            enableExternalAccount(accountId);
        }

        return new EnableAccountsResponse();
    }

    private void enableExternalAccount(Long accountId) {
        ExternalAccount externalAccount = repository.findExternalAccount(accountId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(externalAccount.getId().toString(), externalAccount.getName())
        );

        externalAccount.enable();
        repository.save(externalAccount);
    }

    @Override
    @Transactional
    @ValidateRequest
    public DisableAccountsResponse disableExternalAccounts(DisableAccountsCmd cmd) {
        List<Long> accountIds = cmd.getAccountIds();
        for (Long accountId : accountIds) {
            disableExternalAccount(accountId);
        }
        return new DisableAccountsResponse();
    }

    private void disableExternalAccount(Long accountId) {
        ExternalAccount externalAccount = repository.findExternalAccount(accountId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(externalAccount.getId().toString(), externalAccount.getName())
        );

        externalAccount.disable();
        repository.save(externalAccount);
    }

    private NestedAccountResponse toNestedAccountResponse(ExternalAccount account) {
        NestedAccountResponse nestedAccountResponse = new NestedAccountResponse();

        EntityUtil.copyBasicFields(account, nestedAccountResponse);

        nestedAccountResponse.setProviderId(account.getProviderId());
        nestedAccountResponse.setProviderName(account.getProvider().getName());
        nestedAccountResponse.setName(account.getName());
        nestedAccountResponse.setProperties(account.getNoSensitiveInfoProperties());
        nestedAccountResponse.setDescription(account.getDescription());
        nestedAccountResponse.setDisabled(account.getDisabled());

        nestedAccountResponse.setState(account.getState());
        nestedAccountResponse.setBalance(account.getBalance());

        return nestedAccountResponse;
    }

    private void synchronizeAccount(Long accountId) {
        ExternalAccount account = repository.findExternalAccount(accountId);

        account.synchronizeState();
        account = repository.save(account);

        if(account.getState() != ExternalAccountState.CONNECTED){
            log.warn("State of external account {} is {}.", account.getName(), account.getState());
            return;
        }

        createManageSteps(account);
    }

    private void createManageSteps(ExternalAccount account) {
        Provider provider = account.getProvider();
        List<ResourceHandler> roots = provider.getManagementRoots();

        Queue<ResourceHandler> syncQueue = new LinkedList<>();

        roots.forEach(syncQueue::offer);
        Set<ResourceHandler> visitedSet = new HashSet<>(roots);

        log.info("Creating execution of synchronizing account {}.", account.getName());

        Execution execution = new Execution();

        while (!syncQueue.isEmpty()) {
            ResourceHandler current = syncQueue.poll();
            log.info("Creating manage step of resource type {}.", current.getResourceTypeId());
            ExecutionStep step = createManageStep(account, current);
            execution.addStep(step);
            var sources = current.getCapabilitiesSources();

            for (ResourceHandler source : sources) {
                boolean targetsVisited = visitedSet.containsAll(source.getRequirementsTargets());
                boolean selfVisited = visitedSet.contains(source);
                if (targetsVisited && !selfVisited) {
                    syncQueue.offer(source);
                    visitedSet.add(source);
                }
            }
        }

        AsyncJobContext.current().getAsyncJob().addExecution(execution);
        log.info("Execution of synchronizing account {} submitted.", account.getName());
    }

    private ExecutionStep createManageStep(ExternalAccount account, ResourceHandler current) {
        List<ExternalResource> externalResources = current.describeExternalResources(account);
        Set<String> existedIds = resourceRepository.findAllByFilters(
                ResourceFilters.builder()
                        .accountIds(List.of(account.getId()))
                        .resourceTypes(List.of(current.getResourceTypeId()))
                        .build()
        ).stream().map(Resource::getExternalId).collect(Collectors.toSet());
        ExecutionStep step = new ExecutionStep();
        for (ExternalResource externalResource : externalResources) {
            if(existedIds.contains(externalResource.externalId()))
                continue;

            Task task = createManageTask(current, externalResource);
            step.addTask(task);
        }
        return step;
    }

    private Task createManageTask(ResourceHandler resourceHandler, ExternalResource externalResource) {
        DummyTaskTargetEntity dummyTaskTargetEntity = new DummyTaskTargetEntity(
                "外部资源-%s: %s".formatted(resourceHandler.getResourceTypeName(), externalResource.name())
        );
        ManageExternalResourceTaskInputs taskInputs = new ManageExternalResourceTaskInputs(externalResource);

        return new Task(dummyTaskTargetEntity, ManageExternalResourceTaskHandler.TASK_TYPE, taskInputs);
    }
}
