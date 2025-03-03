package com.stratocloud.limit;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.external.resource.TenantGatewayService;
import com.stratocloud.identity.SimpleTenant;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.Execution;
import com.stratocloud.job.ExecutionStep;
import com.stratocloud.limit.cmd.*;
import com.stratocloud.limit.query.*;
import com.stratocloud.limit.response.*;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ProviderRegistry;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.repository.ResourceUsageLimitRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsageType;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import com.stratocloud.utils.Utils;
import com.stratocloud.validate.ValidateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ResourceUsageLimitServiceImpl implements ResourceUsageLimitService {

    private final ResourceUsageLimitRepository repository;

    private final TenantGatewayService tenantGatewayService;

    private final ResourceUsageLimitAssembler assembler;

    private final ResourceUsageLimitFactory factory;

    public ResourceUsageLimitServiceImpl(ResourceUsageLimitRepository repository,
                                         TenantGatewayService tenantGatewayService,
                                         ResourceUsageLimitAssembler assembler,
                                         ResourceUsageLimitFactory factory) {
        this.repository = repository;
        this.tenantGatewayService = tenantGatewayService;
        this.assembler = assembler;
        this.factory = factory;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public void checkLimits(List<Resource> resourcesToAllocate) {
        List<ResourceUsageLimit> limits = getRelatedLimits(resourcesToAllocate);

        for (ResourceUsageLimit limit : limits)
            limit.check(resourcesToAllocate);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedLimitResponse> describeLimits(DescribeLimitsRequest request) {
        List<Long> tenantIds = request.getTenantIds();
        String search = request.getSearch();
        Pageable pageable = request.getPageable();

        Page<ResourceUsageLimit> page = repository.page(tenantIds, search, pageable);

        return page.map(assembler::toNestedLimitResponse);
    }

    @Override
    @ValidateRequest
    public DescribeUsageTypesResponse describeUsageTypes(DescribeUsageTypesRequest request) {
        List<String> providerIds = request.getProviderIds();
        List<String> resourceCategories = request.getResourceCategories();

        List<Provider> providers = new ArrayList<>();

        if(Utils.isNotEmpty(providerIds)){
            for (String providerId : providerIds) {
                providers.add(ProviderRegistry.getProvider(providerId));
            }
        }else {
            providers.addAll(ProviderRegistry.getProviders());
        }

        List<NestedUsageType> usageTypes = new ArrayList<>();

        for (Provider provider : providers) {
            List<ResourceHandler> resourceHandlers = new ArrayList<>();

            if(Utils.isNotEmpty(resourceCategories)){
                for (String resourceCategory : resourceCategories) {
                    provider.getResourceHandlerByCategory(resourceCategory).ifPresent(resourceHandlers::add);
                }
            }else {
                resourceHandlers.addAll(provider.getResourceHandlers());
            }

            for (ResourceHandler resourceHandler : resourceHandlers) {
                for (ResourceUsageType usagesType : resourceHandler.getUsagesTypes()) {
                    if(usageTypes.stream().anyMatch(ut -> usagesType.type().equals(ut.getUsageType())))
                        continue;

                    usageTypes.add(new NestedUsageType(usagesType.type(), usagesType.name()));
                }
            }
        }

        return new DescribeUsageTypesResponse(usageTypes);
    }



    @Override
    @Transactional
    @ValidateRequest
    public CreateLimitResponse createLimit(CreateLimitCmd cmd) {
        ResourceUsageLimit limit = factory.create(cmd);

        limit = repository.save(limit);

        AuditLogContext.current().addAuditObject(
                new AuditObject(limit.getId().toString(), limit.getName())
        );

        return new CreateLimitResponse(limit.getId());
    }

    @Override
    @Transactional
    @ValidateRequest
    public UpdateLimitResponse updateLimit(UpdateLimitCmd cmd) {
        Long limitId = cmd.getLimitId();

        String name = cmd.getName();
        String description = cmd.getDescription();
        List<Long> ownerIds = cmd.getOwnerIds();
        List<String> providerIds = cmd.getProviderIds();
        List<Long> accountIds = cmd.getAccountIds();
        List<String> resourceCategories = cmd.getResourceCategories();
        List<NestedResourceTag> tags = cmd.getTags();
        List<NestedUsageLimitItem> items = cmd.getItems();

        ResourceUsageLimit limit = repository.findLimit(limitId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(limit.getId().toString(), limit.getName())
        );

        limit.update(name, description, ownerIds, providerIds, accountIds, resourceCategories);

        limit.clearTags();
        limit.addTags(factory.createLimitTags(tags));

        limit.clearItems();
        limit.addItems(factory.createLimitItems(items));

        repository.save(limit);

        return new UpdateLimitResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public EnableLimitsResponse enableLimits(EnableLimitsCmd cmd) {
        for (Long limitId : cmd.getLimitIds()) {
            enableLimit(limitId);
        }
        return new EnableLimitsResponse();
    }

    private void enableLimit(Long limitId) {
        ResourceUsageLimit limit = repository.findLimit(limitId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(limit.getId().toString(), limit.getName())
        );

        limit.enable();
        repository.save(limit);
    }

    @Override
    @Transactional
    @ValidateRequest
    public DisableLimitsResponse disableLimits(DisableLimitsCmd cmd) {
        for (Long limitId : cmd.getLimitIds()) {
            disableLimit(limitId);
        }

        return new DisableLimitsResponse();
    }

    private void disableLimit(Long limitId) {
        ResourceUsageLimit limit = repository.findLimit(limitId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(limit.getId().toString(), limit.getName())
        );

        limit.disable();
        repository.save(limit);
    }

    @Override
    @Transactional
    @ValidateRequest
    public DeleteLimitsResponse deleteLimits(DeleteLimitsCmd cmd) {
        cmd.getLimitIds().forEach(this::deleteLimit);
        return new DeleteLimitsResponse();
    }

    private void deleteLimit(Long limitId) {
        ResourceUsageLimit limit = repository.findLimit(limitId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(limit.getId().toString(), limit.getName())
        );

        repository.delete(limit);
    }

    private List<ResourceUsageLimit> getRelatedLimits(List<Resource> resourcesToAllocate) {
        if(Utils.isEmpty(resourcesToAllocate))
            return List.of();

        Set<Long> tenantIds = new HashSet<>();
        for (Resource resourceToAllocate : resourcesToAllocate) {
            tenantIds.addAll(getInheritedTenantIds(resourceToAllocate.getTenantId()));
            tenantIds.add(resourceToAllocate.getTenantId());
        }
        return repository.findAllByTenantIds(new ArrayList<>(tenantIds));
    }

    private List<Long> getInheritedTenantIds(Long tenantId) {
        List<SimpleTenant> inheritedTenants
                = tenantGatewayService.findInheritedTenants(tenantId);
        List<Long> tenantIds = new ArrayList<>();
        inheritedTenants.forEach(it -> tenantIds.add(it.id()));
        return tenantIds;
    }


    @Override
    public void synchronizeAllLimits(){
        List<ResourceUsageLimit> limits = repository.findAll();

        if(Utils.isEmpty(limits))
            return;

        log.info("Synchronizing {} resource usage limits.", limits.size());

        Execution execution = createSynchronizeExecution(limits);
        AsyncJobContext.current().getAsyncJob().addExecution(execution);
    }

    private Execution createSynchronizeExecution(List<ResourceUsageLimit> limits) {
        Execution execution = new Execution();
        ExecutionStep step = new ExecutionStep();
        for (ResourceUsageLimit limit : limits) {
            step.addTask(limit.createSynchronizeTask());
        }
        execution.addStep(step);
        return execution;
    }
}
