package com.stratocloud.resource;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.account.ExternalAccountState;
import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.auth.CallContext;
import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.Execution;
import com.stratocloud.job.ExecutionStep;
import com.stratocloud.job.Task;
import com.stratocloud.limit.ResourceUsageLimitService;
import com.stratocloud.orchestrator.Orchestrator;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ProviderRegistry;
import com.stratocloud.provider.relationship.ChangeableEssentialHandler;
import com.stratocloud.provider.relationship.DependsOnRelationshipHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.provider.resource.monitor.MonitoredResourceHandler;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.repository.RelationshipRepository;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.cmd.*;
import com.stratocloud.resource.cmd.action.RunActionCmd;
import com.stratocloud.resource.cmd.create.CreateResourcesCmd;
import com.stratocloud.resource.cmd.create.NestedNewResource;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import com.stratocloud.resource.cmd.ownership.TransferCmd;
import com.stratocloud.resource.cmd.recycle.RecycleCmd;
import com.stratocloud.resource.cmd.relationship.*;
import com.stratocloud.resource.monitor.ResourceQuickStats;
import com.stratocloud.resource.query.*;
import com.stratocloud.resource.query.inquiry.*;
import com.stratocloud.resource.query.metadata.*;
import com.stratocloud.resource.response.*;
import com.stratocloud.utils.GraphUtil;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.ConcurrentUtil;
import com.stratocloud.validate.ValidateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository repository;

    private final RelationshipRepository relationshipRepository;

    private final Orchestrator orchestrator;

    private final ResourceFactory resourceFactory;

    private final ResourceUsageLimitService limitService;

    private final ResourceAssembler assembler;

    private final ExternalAccountRepository accountRepository;

    private final CacheService cacheService;

    public ResourceServiceImpl(ResourceRepository repository,
                               RelationshipRepository relationshipRepository,
                               Orchestrator orchestrator,
                               ResourceFactory resourceFactory,
                               ResourceUsageLimitService limitService,
                               ResourceAssembler assembler,
                               ExternalAccountRepository accountRepository,
                               CacheService cacheService) {
        this.repository = repository;
        this.relationshipRepository = relationshipRepository;
        this.orchestrator = orchestrator;
        this.resourceFactory = resourceFactory;
        this.limitService = limitService;
        this.assembler = assembler;
        this.accountRepository = accountRepository;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional
    @ValidateRequest
    public CreateResourcesResponse create(BatchCreateResourcesCmd cmd) {
        List<CreateResourcesCmd> createResourcesCmdList = cmd.getResources();

        List<Resource> allResources = new ArrayList<>();

        for (CreateResourcesCmd createResourcesCmd : createResourcesCmdList) {
            List<Resource> resources = create(createResourcesCmd);
            allResources.addAll(resources);
        }

        checkLimitsForCreation(allResources);

        List<Long> resourceIds = allResources.stream().map(Resource::getId).toList();

        return new CreateResourcesResponse(resourceIds);
    }

    @Override
    @Transactional
    @ValidateRequest
    public RunActionsResponse runActions(BatchRunActionsCmd cmd) {
        List<RunActionCmd> runActionCmdList = cmd.getActions();

        Map<Long, Resource> resourcesToRunActionsMap = new HashMap<>();
        for (RunActionCmd runActionCmd : runActionCmdList) {
            Resource resourceToRunAction = runAction(runActionCmd);
            resourcesToRunActionsMap.put(resourceToRunAction.getId(), resourceToRunAction);
        }
        limitService.checkLimits(new ArrayList<>(resourcesToRunActionsMap.values()));

        return new RunActionsResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public RecycleResourcesResponse recycle(BatchRecycleCmd cmd) {
        List<RecycleCmd> resources = cmd.getResources();

        for (RecycleCmd resource : resources) {
            recycle(resource);
        }

        return new RecycleResourcesResponse();
    }

    @Override
    @Transactional
    @ValidateRequest
    public RestoreResourcesResponse restore(BatchRestoreCmd cmd) {
        List<Long> resourceIds = cmd.getResourceIds();

        for (Long resourceId : resourceIds) {
            restore(resourceId);
        }

        return new RestoreResourcesResponse();
    }

    private void restore(Long resourceId) {
        Resource resource = repository.findResource(resourceId);
        resource.restore();
        repository.save(resource);
    }

    @Override
    @Transactional
    @ValidateRequest
    public TransferResourcesResponse transfer(BatchTransferCmd cmd) {
        for (TransferCmd transfer : cmd.getTransfers()) {
            transferResource(transfer);
        }
        return new TransferResourcesResponse();
    }

    private void transferResource(TransferCmd transfer) {
        Long resourceId = transfer.getResourceId();
        Long newTenantId = transfer.getNewTenantId();
        Long newOwnerId = transfer.getNewOwnerId();

        Resource resource = repository.findResource(resourceId);

        resource.transferTo(newTenantId, newOwnerId, transfer.getEnableCascadedTransfer());

        repository.saveWithSystemSession(resource);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedResourceResponse> describeResources(DescribeResourcesRequest request) {
        ResourceFilters filters = createResourceFilters(request);

        Page<Resource> page = repository.page(filters, request.getPageable());

        return assembler.convertPage(page);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedResourceResponse> describeUnclaimedResources(DescribeUnclaimedResourcesRequest request) {
        String category = request.getCategory();
        String search = request.getSearch();
        List<Long> resourceIds = request.getResourceIds();
        Pageable pageable = request.getPageable();

        Page<Resource> page = repository.pageUnclaimed(category, search, resourceIds, pageable);
        return assembler.convertPage(page);
    }

    @Override
    @Transactional
    @ValidateRequest
    public SynchronizeResourcesResponse synchronizeResources(SynchronizeResourcesCmd cmd) {
        List<Long> resourceIds = cmd.getResourceIds();
        Boolean synchronizeAll = cmd.getSynchronizeAll();

        if(synchronizeAll != null && synchronizeAll)
            synchronizeAllResources();
        else
            synchronizeResources(resourceIds);

        return new SynchronizeResourcesResponse();
    }

    private static List<ResourceAction> sortActions(Set<ResourceAction> actions){
        return actions.stream().sorted(Comparator.comparingInt(ResourceAction::index)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public DescribeAvailableActionsResponse describeResourceActions(DescribeAvailableActionsRequest request) {
        List<Long> resourceIds = request.getResourceIds();
        String category = request.getCategory();

        if(Utils.isEmpty(resourceIds)){
            Set<ResourceAction> actions = getAllActionsByCategory(category);
            return new DescribeAvailableActionsResponse(
                    sortActions(actions),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }


        List<Resource> resources = repository.findAllById(resourceIds);

        if(Utils.isEmpty(resources)) {
            Set<ResourceAction> actions = getAllActionsByCategory(category);
            return new DescribeAvailableActionsResponse(
                    sortActions(actions),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }
        Resource firstResource = resources.get(0);

        Set<ResourceAction> availableActions = new HashSet<>(firstResource.getAvailableActions());
        Set<ResourceAction> availableReadActions = new HashSet<>(firstResource.getAvailableReadActions());

        for (int i = 1; i < resources.size(); i++) {
            availableActions.retainAll(resources.get(i).getAvailableActions());
            availableReadActions.retainAll(resources.get(i).getAvailableReadActions());
        }

        Set<ResourceAction> allActions = getAllActionsByCategory(firstResource.getCategory());


        return new DescribeAvailableActionsResponse(
                sortActions(allActions),
                sortActions(availableActions),
                sortActions(availableReadActions)
        );
    }

    private Set<ResourceAction> getAllActionsByCategory(String category) {
        Set<ResourceAction> result = new HashSet<>();
        List<Provider> providers = ProviderRegistry.getProviders();
        for (Provider provider : providers) {
            List<? extends ResourceHandler> resourceHandlers = provider.getResourceHandlers().stream().filter(
                    resourceHandler -> resourceHandler.getResourceCategory().id().equals(category)
            ).toList();

            for (ResourceHandler resourceHandler : resourceHandlers) {
                List<ResourceAction> actions
                        = resourceHandler.getActionHandlers().stream().map(ResourceActionHandler::getAction).toList();
                result.addAll(actions);
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedRelationshipResponse> describeRequirements(DescribeRequirementsRequest request) {
        Long sourceId = request.getSourceId();
        String relationshipType = request.getRelationshipType();
        String search = request.getSearch();
        Pageable pageable = request.getPageable();

        Page<Relationship> page = relationshipRepository.pageRequirements(
                sourceId, relationshipType, search, pageable
        );

        return page.map(assembler::toNestedRelationshipResponse);
    }


    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedRelationshipResponse> describeCapabilities(DescribeCapabilitiesRequest request) {
        Long targetId = request.getTargetId();
        String relationshipType = request.getRelationshipType();
        String search = request.getSearch();
        Pageable pageable = request.getPageable();

        Page<Relationship> page = relationshipRepository.pageCapabilities(
                targetId, relationshipType, search, pageable
        );

        return page.map(assembler::toNestedRelationshipResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public Page<NestedRelationshipResponse> describeRelationships(DescribeRelationshipsRequest request) {
        List<Long> relationshipIds = request.getRelationshipIds();
        Pageable pageable = request.getPageable();

        Page<Relationship> page =relationshipRepository.page(relationshipIds, pageable);

        return page.map(assembler::toNestedRelationshipResponse);
    }

    @Override
    @Transactional
    @ValidateRequest
    public ConnectResourcesResponse connectResources(BatchConnectResourcesCmd cmd) {
        for (ConnectResourcesCmd connection : cmd.getConnections()) {
            connectResources(connection);
        }
        return new ConnectResourcesResponse();
    }

    private void connectResources(ConnectResourcesCmd connection) {
        Long sourceResourceId = connection.getSourceResourceId();
        Long targetResourceId = connection.getTargetResourceId();
        String relationshipTypeId = connection.getRelationshipTypeId();
        Map<String, Object> relationshipInputs = connection.getRelationshipInputs();

        Resource source = repository.findResource(sourceResourceId);
        Resource target = repository.findResource(targetResourceId);

        Execution execution = new Execution();

        Relationship rel = source.addRequirement(target, relationshipTypeId, relationshipInputs);

        rel = relationshipRepository.save(rel);

        Task connectTask = rel.createConnectTask();

        execution.addStep(ExecutionStep.of(connectTask));

        execution.addStep(ExecutionStep.of(source.createSynchronizeTask()));

        if(rel.getHandler().synchronizeTarget())
            execution.addStep(ExecutionStep.of(rel.getTarget().createSynchronizeTask()));

        AsyncJobContext.current().getAsyncJob().addExecution(execution);

        repository.save(source);
    }

    @Override
    @Transactional
    @ValidateRequest
    public ChangeEssentialRequirementsResponse changeEssentialRequirements(BatchChangeEssentialRequirementsCmd cmd) {
        for (ChangeEssentialRequirementCmd change : cmd.getChanges()) {
            changeEssentialRequirement(change);
        }
        return new ChangeEssentialRequirementsResponse();
    }


    @Override
    @ValidateRequest
    public DescribeResourceTypesResponse describeResourceTypes(DescribeResourceTypesRequest request) {
        List<ResourceHandler> resourceHandlers = getResourceHandlers(request);

        List<NestedResourceType> nestedResourceTypes = resourceHandlers.stream().map(
                assembler::toNestedResourceType
        ).toList();

        return new DescribeResourceTypesResponse(nestedResourceTypes);
    }

    @Override
    @ValidateRequest
    public DescribeResourceCategoriesResponse describeResourceCategories(DescribeResourceCategoriesRequest request) {
        Set<ResourceCategory> categories = new HashSet<>();

        for (Provider provider : ProviderRegistry.getProviders()) {
            if(Utils.isNotBlank(request.getProviderId()) && !request.getProviderId().equals(provider.getId()))
                continue;

            for (ResourceHandler resourceHandler : provider.getResourceHandlers()) {
                if(Utils.isNotBlank(request.getCategoryId()) &&
                        !request.getCategoryId().equals(resourceHandler.getResourceCategory().id()))
                    continue;

                PermissionItem permissionItem = resourceHandler.getPermissionItem();
                if(CallContext.current().hasPermission(permissionItem.target(), permissionItem.action()))
                    categories.add(resourceHandler.getResourceCategory());
            }
        }

        ArrayList<ResourceCategory> categoryList = new ArrayList<>(categories);
        categoryList.sort(Comparator.comparingInt(ResourceCategory::index));

        List<NestedResourceCategory> nestedResourceCategories = categoryList.stream().map(
                assembler::toNestedResourceCategory
        ).toList();

        return new DescribeResourceCategoriesResponse(nestedResourceCategories);
    }


    @Override
    @ValidateRequest
    public DescribeProvidersResponse describeProviders(DescribeProvidersRequest request) {
        List<String> providerIds = request.getProviderIds();
        String resourceCategory = request.getResourceCategory();

        List<Provider> providers = ProviderRegistry.getProviders();
        Stream<Provider> stream = providers.stream();

        if(Utils.isNotEmpty(providerIds))
            stream = stream.filter(provider -> providerIds.contains(provider.getId()));

        if(Utils.isNotBlank(resourceCategory))
            stream = stream.filter(provider -> provider.getResourceHandlerByCategory(resourceCategory).isPresent());

        return new DescribeProvidersResponse(stream.map(assembler::toProviderResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    @ValidateRequest
    public DescribeResourceActionFormResponse describeResourceActionForm(DescribeResourceActionFormRequest request) {
        Long resourceId = request.getResourceId();
        String resourceTypeId = request.getResourceTypeId();
        String actionId = request.getActionId();

        Long networkResourceId = null;

        if(resourceId != null) {
            Resource resource = repository.findResource(resourceId);
            resourceTypeId = resource.getType();

            Optional<Resource> networkResource = resource.getRequirements().stream().filter(
                    r -> r.getState() == RelationshipState.CONNECTED
            ).map(Relationship::getTarget).filter(
                    t -> t.getResourceHandler().canAttachIpPool()
            ).findAny();

            if(networkResource.isPresent())
                networkResourceId = networkResource.get().getId();
        }

        ResourceHandler resourceHandler = ProviderRegistry.getResourceHandler(resourceTypeId);
        Optional<ResourceActionHandler> actionHandler = resourceHandler.getActionHandler(actionId);

        if(actionHandler.isEmpty())
            throw new BadCommandException(
                    "Unsupported action %s for resource type %s.".formatted(actionId, resourceTypeId)
            );

        var directInputClassDynamicFormMetaData = actionHandler.get().getDirectInputClassDynamicFormMetaData();

        if(directInputClassDynamicFormMetaData.isPresent()){
            return new DescribeResourceActionFormResponse(
                    directInputClassDynamicFormMetaData.get(),
                    networkResourceId
            );
        }

        Class<? extends ResourceActionInput> inputClass = actionHandler.get().getInputClass();

        return new DescribeResourceActionFormResponse(
                DynamicFormHelper.generateMetaData(inputClass),
                networkResourceId
        );
    }

    @Override
    @ValidateRequest
    public DescribeRelationshipFormResponse describeRelationshipForm(DescribeRelationshipFormRequest request) {
        String relationshipTypeId = request.getRelationshipTypeId();

        RelationshipHandler relationshipHandler = ProviderRegistry.getRelationshipHandler(relationshipTypeId);

        var directConnectInputClassFormMetaData = relationshipHandler.getDirectConnectInputClassFormMetaData();

        if(directConnectInputClassFormMetaData.isPresent())
            return new DescribeRelationshipFormResponse(
                    directConnectInputClassFormMetaData.get()
            );


        var connectInputClass = relationshipHandler.getConnectInputClass();

        return new DescribeRelationshipFormResponse(DynamicFormHelper.generateMetaData(connectInputClass));
    }

    @Override
    @ValidateRequest
    public DescribeRelationshipSpecResponse describeRelationshipSpec(DescribeRelationshipSpecRequest request) {
        String relationshipTypeId = request.getRelationshipTypeId();

        RelationshipHandler relationshipHandler = ProviderRegistry.getRelationshipHandler(relationshipTypeId);

        return new DescribeRelationshipSpecResponse(assembler.toNestedRelationshipSpec(relationshipHandler));
    }


    @Override
    @ValidateRequest
    public DescribeRelationshipTypesResponse describeRelationshipTypes(DescribeRelationshipTypesRequest request) {
        String sourceTypeId = request.getSourceTypeId();
        String targetTypeId = request.getTargetTypeId();

        ResourceHandler sourceHandler = ProviderRegistry.getResourceHandler(sourceTypeId);
        ResourceHandler targetHandler = ProviderRegistry.getResourceHandler(targetTypeId);

        List<? extends RelationshipHandler> relationshipHandlers = sourceHandler.getRequirements().stream().filter(
                r -> r.getTarget().getResourceTypeId().equals(targetTypeId)
        ).toList();

        List<NestedRelationshipSpec> specs = new ArrayList<>();

        relationshipHandlers.forEach(r -> specs.add(assembler.toNestedRelationshipSpec(r)));

        specs.add(assembler.toNestedRelationshipSpec(new DependsOnRelationshipHandler(sourceHandler, targetHandler)));

        return new DescribeRelationshipTypesResponse(specs);
    }

    @Override
    @Transactional
    @ValidateRequest
    public DisconnectResourcesResponse disconnectResources(BatchDisconnectResourcesCmd cmd) {
        List<Long> relationshipIds = cmd.getRelationshipIds();

        for (Long relationshipId : relationshipIds) {
            disconnect(relationshipId);
        }

        return new DisconnectResourcesResponse();
    }


    @Override
    @ValidateRequest
    public CreateResourcesPriceInquiryResponse performCreateResourcesPriceInquiry(CreateResourcesPriceInquiry inquiry) {
        CreateResourcesPriceInquiryResponse response = new CreateResourcesPriceInquiryResponse();

        Resource resource;
        try{
            resource = resourceFactory.createResource(inquiry.getCreateCommand(), false);
        }catch (Exception e){
            log.debug("Cannot perform price inquiry: {}.", e.getMessage(), e);
            return response;
        }

        List<Resource> resourcesToBuild = GraphUtil.bfs(
                resource,
                r -> r.getCapabilities().stream().map(Relationship::getSource).toList()
        );

        ResourceCost sum = ResourceCost.ZERO;

        for (Resource resourceToBuild : resourcesToBuild) {
            Optional<ResourceActionHandler> buildHandler
                    = resourceToBuild.getResourceHandler().getActionHandler(ResourceActions.BUILD_RESOURCE);

            if(buildHandler.isEmpty())
                continue;

            try {
                sum = sum.add(buildHandler.get().getActionCost(resourceToBuild, resourceToBuild.getProperties()));
            }catch (Exception e){
                log.debug("Cannot perform price inquiry: {}.", e.getMessage(), e);
            }
        }

        Integer number = inquiry.getCreateCommand().getNumber();
        if(number != null)
            sum = sum.multiply(number);


        response.setTotalCostDescription(sum.toDescription());
        response.setTotalMonthlyCostDescription(sum.toMonthlyCostDescription());

        return response;
    }

    @Override
    @ValidateRequest
    public DestroyResourcesRefundInquiryResponse performDestroyResourcesRefundInquiry(DestroyResourcesRefundInquiry inquiry) {
        DestroyResourcesRefundInquiryResponse response = new DestroyResourcesRefundInquiryResponse();

        ResourceCost sum = ResourceCost.ZERO;


        for (RecycleCmd recycleCmd : inquiry.getResources()) {
            Long resourceId = recycleCmd.getResourceId();
            Map<String, Object> destroyParameters = recycleCmd.getDestroyParameters();
            Resource resource = repository.findResource(resourceId);

            ResourceCost refund = getRefund(resource, destroyParameters, recycleCmd.isRecyclingCapabilities());

            sum = sum.absoluteAdd(refund);
        }

        response.setRefundDescription(sum.toDescription());

        return response;
    }


    @Override
    @ValidateRequest
    public RunActionsPriceInquiryResponse performRunActionsPriceInquiry(RunActionsPriceInquiry inquiry) {
        ResourceCost sum = ResourceCost.ZERO;

        for (RunActionCmd actionCmd : inquiry.getActions()) {
            Long resourceId = actionCmd.getResourceId();
            String actionId = actionCmd.getActionId();
            Map<String, Object> parameters = actionCmd.getParameters();

            Resource resource = repository.findResource(resourceId);
            ResourceActionHandler actionHandler = resource.getActionHandler(actionId);

            sum = sum.add(actionHandler.getActionCost(resource, parameters));
        }

        RunActionsPriceInquiryResponse response = new RunActionsPriceInquiryResponse();

        response.setTotalCostDescription(sum.toDescription());
        response.setTotalMonthlyCostDescription(sum.toMonthlyCostDescription());

        return response;
    }

    @Override
    @ValidateRequest
    public ChangeEssentialRequirementsPriceInquiryResponse performChangeEssentialRequirementsPriceInquiry(
            ChangeEssentialRequirementsPriceInquiry inquiry
    ) {
        ResourceCost sum = ResourceCost.ZERO;

        for (var changeCmd : inquiry.getChanges()) {
            Long sourceId = changeCmd.getSourceId();
            Long newTargetId = changeCmd.getNewTargetId();
            String relationshipTypeId = changeCmd.getRelationshipTypeId();
            Map<String, Object> relationshipInputs = changeCmd.getRelationshipInputs();

            if(sourceId ==null || newTargetId == null || Utils.isBlank(relationshipTypeId))
                continue;

            Resource source = repository.findResource(sourceId);
            Resource newTarget = repository.findResource(newTargetId);

            ChangeableEssentialHandler handler
                    = (ChangeableEssentialHandler) source.getResourceHandler().getRequirement(relationshipTypeId);



            sum = sum.add(handler.getChangeCost(source, newTarget, relationshipInputs));
        }

        var response = new ChangeEssentialRequirementsPriceInquiryResponse();

        response.setTotalCostDescription(sum.toDescription());
        response.setTotalMonthlyCostDescription(sum.toMonthlyCostDescription());

        return response;
    }

    private ResourceCost getRefund(Resource resource,
                                   Map<String, Object> destroyParameters,
                                   boolean recyclingCapabilities) {
        ResourceCost refund = ResourceCost.ZERO;


        Optional<ResourceActionHandler> destroyHandler
                = resource.getResourceHandler().getActionHandler(ResourceActions.DESTROY_RESOURCE);


        try {
            if(destroyHandler.isPresent())
                refund = refund.absoluteAdd(destroyHandler.get().getActionCost(resource, destroyParameters));

            if(recyclingCapabilities){
                for (Relationship capability : resource.getCapabilities()) {
                    refund = refund.absoluteAdd(getRefund(capability.getSource(), Map.of(), true));
                }
            }
        }catch (Exception e){
            log.debug("Cannot perform refund inquiry: {}.", e.getMessage(), e);
        }

        return refund;
    }

    private void disconnect(Long relationshipId) {
        Relationship relationship = relationshipRepository.findRelationship(relationshipId);
        Task disconnectTask = relationship.createDisconnectTask();

        Execution execution = new Execution();
        execution.addStep(ExecutionStep.of(disconnectTask));
        execution.addStep(ExecutionStep.of(relationship.getSource().createSynchronizeTask()));

        if(relationship.getHandler().synchronizeTarget())
            execution.addStep(ExecutionStep.of(relationship.getTarget().createSynchronizeTask()));

        AsyncJobContext.current().getAsyncJob().addExecution(execution);

        relationshipRepository.save(relationship);
    }


    private static List<ResourceHandler> getResourceHandlers(DescribeResourceTypesRequest request) {
        String resourceCategoryId = request.getResourceCategoryId();
        String providerId = request.getProviderId();
        String resourceTypeId = request.getResourceTypeId();

        Stream<Provider> providerStream = ProviderRegistry.getProviders().stream();

        if(Utils.isNotBlank(providerId)){
            providerStream = providerStream.filter(provider -> Objects.equals(providerId, provider.getId()));
        }

        List<Provider> providers = providerStream.toList();

        List<ResourceHandler> resourceHandlers = new ArrayList<>();

        for (Provider provider : providers) {
            Stream<? extends ResourceHandler> stream = provider.getResourceHandlers().stream();
            if(Utils.isNotBlank(resourceCategoryId))
                stream = stream.filter(
                        resourceHandler -> Objects.equals(
                                resourceCategoryId,
                                resourceHandler.getResourceCategory().id()
                        )
                );

            if(Utils.isNotBlank(resourceTypeId))
                stream = stream.filter(
                        resourceHandler -> Objects.equals(
                                resourceTypeId,
                                resourceHandler.getResourceTypeId()
                        )
                );

            resourceHandlers.addAll(stream.toList());
        }
        return resourceHandlers.stream().sorted(
                Comparator.comparingInt(rh -> rh.getResourceCategory().index())
        ).toList();
    }

    private void changeEssentialRequirement(ChangeEssentialRequirementCmd change) {
        Long sourceId = change.getSourceId();
        String relationshipTypeId = change.getRelationshipTypeId();
        Long newTargetId = change.getNewTargetId();
        Map<String, Object> relationshipInputs = change.getRelationshipInputs();

        Resource source = repository.findResource(sourceId);

        Optional<Relationship> oldRelOptional = source.getEssentialRequirements().stream().filter(
                er -> Objects.equals(er.getType(), relationshipTypeId)
        ).findAny();

        if(oldRelOptional.isEmpty())
            throw new StratoException(
                    "Old changeable relationship not found by type %s.".formatted(relationshipTypeId)
            );

        Relationship oldRel = oldRelOptional.get();

        Resource newTarget = repository.findResource(newTargetId);

        Execution execution = new Execution();

        Task disconnectTask = oldRel.createDisconnectTask();

        execution.addStep(ExecutionStep.of(disconnectTask));

        Relationship newRel = source.addRequirement(newTarget, oldRel.getType(), relationshipInputs);

        source = repository.save(source);

        Task connectTask = newRel.createConnectTask();

        execution.addStep(ExecutionStep.of(connectTask));

        execution.addStep(ExecutionStep.of(source.createSynchronizeTask()));

        AsyncJobContext.current().getAsyncJob().addExecution(execution);

        repository.save(source);
    }

    private void synchronizeResources(List<Long> resourceIds) {
        if(Utils.isEmpty(resourceIds))
            return;

        List<Resource> resources = repository.findAllById(resourceIds);

        Execution execution = createSynchronizeExecution(resources);
        AsyncJobContext.current().getAsyncJob().addExecution(execution);
    }

    private Execution createSynchronizeExecution(List<Resource> resources) {
        Execution execution = new Execution();
        ExecutionStep step = new ExecutionStep();
        for (Resource resource : resources) {
            Task task = resource.createSynchronizeTask();
            step.addTask(task);
        }
        execution.addStep(step);
        return execution;
    }

    private void synchronizeAllResources() {
        ResourceFilters filters = ResourceFilters.builder().states(ResourceState.getAliveStates()).build();

        List<Resource> resources = repository.findAllByFilters(filters);
        Execution execution = createSynchronizeExecution(resources);
        AsyncJobContext.current().getAsyncJob().addExecution(execution);
    }

    public ResourceFilters createResourceFilters(DescribeResourcesRequest request) {
        var builder = ResourceFilters.builder();
        return builder.tenantIds(request.getTenantIds())
                .resourceIds(request.getResourceIds())
                .requirementTargetIds(request.getRequirementTargetIds())
                .states(request.getStates())
                .syncStates(request.getSyncStates())
                .search(request.getSearch())
                .recycled(request.getRecycled())
                .providerIds(request.getProviderIds())
                .accountIds(request.getAccountIds())
                .resourceCategories(request.getResourceCategories())
                .resourceTypes(request.getResourceTypes())
                .tagsMap(request.getTagsMap())
                .ownerIds(request.getOwnerIds())
                .ipPoolAttachable(request.getIpPoolAttachable())
                .build();
    }


    private void recycle(RecycleCmd recycleCmd) {
        Long resourceId = recycleCmd.getResourceId();
        boolean recyclingCapabilities = recycleCmd.isRecyclingCapabilities();
        boolean executingDestruction = recycleCmd.isExecutingDestruction();
        Map<String, Object> destroyParameters = recycleCmd.getDestroyParameters();

        Resource resource = repository.findResource(resourceId);

        if(executingDestruction){
            resource.validateActionPrecondition(ResourceActions.DESTROY_RESOURCE.id(), destroyParameters);
            Execution execution = orchestrator.orchestrateDestruction(resource, destroyParameters, recyclingCapabilities);
            AsyncJobContext.current().getAsyncJob().addExecution(execution);
        }

        resource.markRecycled(recyclingCapabilities);

        repository.save(resource);
    }

    private Resource runAction(RunActionCmd runActionCmd) {
        Long resourceId = runActionCmd.getResourceId();
        String actionId = runActionCmd.getActionId();
        Map<String, Object> parameters = runActionCmd.getParameters();

        Resource resource = repository.findResource(resourceId);

        resource.validateActionPrecondition(actionId, parameters);

        Task task = resource.createResourceTask(actionId, parameters);

        AsyncJobContext.current().getAsyncJob().addExecution(Execution.of(task));

        return repository.save(resource);
    }






    private List<Resource> create(CreateResourcesCmd createResourcesCmd) {
        return splitByNumber(createResourcesCmd).stream().map(this::createSingleResource).toList();
    }


    private static List<CreateResourcesCmd> splitByNumber(CreateResourcesCmd createResourcesCmd) {
        int number = createResourcesCmd.getNumber();

        if(number == 1)
            return List.of(createResourcesCmd);

        List<CreateResourcesCmd> splitList = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            CreateResourcesCmd clonedCmd = JSON.clone(createResourcesCmd);

            clonedCmd.setNumber(1);

            for (NestedNewResource resource : clonedCmd.getAllResourcesRecursively()) {
                Map<String, Object> splitProperties = ProviderRegistry
                        .getResourceHandler(resource.getResourceTypeId())
                        .getPropertiesAtIndex(resource.getProperties(), i);
                resource.setProperties(splitProperties);
            }

            splitList.add(clonedCmd);
        }
        return splitList;
    }

    private Resource createSingleResource(CreateResourcesCmd createResourcesCmd) {
        Resource resource = resourceFactory.createResource(createResourcesCmd);

        resource = repository.save(resource);

        checkBuildPreconditions(resource);

        Execution execution = orchestrator.orchestrateBuild(resource);

        AsyncJobContext.current().getAsyncJob().addExecution(execution);

        resource = repository.save(resource);

        return resource;
    }

    private static void checkBuildPreconditions(Resource resource) {
        List<Resource> resourcesToBuild = GraphUtil.bfs(
                resource,
                r -> r.getCapabilities().stream().map(Relationship::getSource).toList()
        );

        List<Runnable> checkTasks = resourcesToBuild.stream().map(
                r -> (Runnable) () -> r.validateActionPrecondition(
                        ResourceActions.BUILD_RESOURCE.id(), r.getProperties()
                )
        ).toList();

        List<ExecutionException> errors = ConcurrentUtil.runAndWaitAndGetErrors(checkTasks);

        if(Utils.isNotEmpty(errors)) {
            String errorMessage = String.join(
                    "\n",
                    errors.stream().map(e -> e.getCause().getMessage()).toList()
            );
            throw new BadCommandException(errorMessage);
        }
    }

    private void checkLimitsForCreation(List<Resource> resources) {
        if(Utils.isEmpty(resources))
            return;

        List<Resource> allResourcesToAllocate = new ArrayList<>();

        for (Resource resource : resources) {
            List<Resource> resourcesToAllocate = GraphUtil.bfs(
                    resource,
                    r -> r.getCapabilities().stream().map(Relationship::getSource).toList()
            );
            allResourcesToAllocate.addAll(resourcesToAllocate);
        }

        limitService.checkLimits(allResourcesToAllocate);
    }


    @Override
    @Transactional
    @ValidateRequest
    public RunReadActionsResponse runReadActions(RunReadActionsCmd cmd) {
        List<RunReadActionsResponse.NestedReadActionResponse> result = new ArrayList<>();

        for (Long resourceId : cmd.getResourceIds()) {
            result.add(runReadAction(resourceId, cmd.getActionId()));
        }

        return new RunReadActionsResponse(result);
    }

    private RunReadActionsResponse.NestedReadActionResponse runReadAction(Long resourceId, String actionId) {
        Resource resource = repository.findResource(resourceId);

        ResourceReadActionHandler readActionHandler = resource.getReadActionHandler(actionId);

        AuditLogContext.current().setSpecificAction(
                readActionHandler.getAction().id(),
                readActionHandler.getAction().name()
        );
        AuditLogContext.current().addAuditObject(
                new AuditObject(resource.getId().toString(), resource.getName())
        );

        List<ResourceReadActionResult> resultList = readActionHandler.performReadAction(resource);

        var response = new RunReadActionsResponse.NestedReadActionResponse();

        response.setResourceId(resourceId);
        response.setResourceName(resource.getName());
        response.setResultList(resultList);

        return response;
    }


    @Override
    @Transactional
    @ValidateRequest
    public SynchronizeResourceStatesResponse synchronizeResourceStates(SynchronizeResourceStatesCmd cmd) {
        var response = new SynchronizeResourceStatesResponse();

        List<ExternalAccount> accounts = accountRepository.findAll();

        if(Utils.isEmpty(accounts))
            return response;

        for (ExternalAccount account : accounts) {
            if(account.getDisabled() || account.getState() != ExternalAccountState.CONNECTED) {
                log.warn("External account {} is in {} state, skipping sync resource states...",
                        account.getName(), account.getState());
                continue;
            }

            try {
                synchronizeResourceStatesByAccount(account);
            }catch (Exception e){
                log.warn("Failed to sync resource states from account {}.", account.getName(), e);
            }
        }

        return response;
    }

    @Override
    @Transactional
    @ValidateRequest
    public DropResourcesResponse dropResources(BatchDropCmd cmd) {
        cmd.getResourceIds().forEach(this::dropResource);
        return new DropResourcesResponse();
    }

    private void dropResource(Long resourceId) {
        Resource resource = repository.findResource(resourceId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(resource.getId().toString(), resource.getName())
        );

        repository.delete(resource);
    }

    @Override
    @Transactional
    @ValidateRequest
    public DescribeQuickStatsResponse describeResourceQuickStats(DescribeQuickStatsRequest request) {
        Resource resource = repository.findResource(request.getResourceId());

        ResourceHandler resourceHandler = resource.getResourceHandler();

        DescribeQuickStatsResponse response = new DescribeQuickStatsResponse();

        if(resourceHandler instanceof MonitoredResourceHandler monitoredResourceHandler){
            if(!ResourceState.getAliveStateSet().contains(resource.getState()))
                return response;

            ResourceQuickStats quickStats = CacheUtil.queryWithCache(
                    cacheService,
                    "QuickStatsOf-%s".formatted(resource.getId()),
                    5,
                    () -> monitoredResourceHandler.describeQuickStats(resource).orElse(null),
                    ResourceQuickStats.builder().build()
            );

            response.setQuickStats(quickStats);
        }

        return response;
    }

    @Override
    @ValidateRequest
    @Transactional
    public AssociateTagsResponse associateTags(AssociateTagsCmd cmd) {
        if(Utils.isEmpty(cmd.getTags()))
            return new AssociateTagsResponse();

        Resource resource = repository.findResource(cmd.getResourceId());

        for (NestedResourceTag tag : cmd.getTags()) {
            resource.updateTag(tag.getTagKey(), tag.getTagKeyName(), tag.getTagValue(), tag.getTagValueName());
        }

        AuditLogContext.current().addAuditObject(
                new AuditObject(resource.getId().toString(), resource.getName())
        );

        repository.save(resource);

        return new AssociateTagsResponse();
    }

    @Override
    @ValidateRequest
    @Transactional
    public DisassociateTagResponse disassociateTag(DisassociateTagCmd cmd) {
        Resource resource = repository.findResource(cmd.getResourceId());

        resource.removeTag(cmd.getTagKey(), cmd.getTagValue());

        AuditLogContext.current().addAuditObject(
                new AuditObject(resource.getId().toString(), resource.getName())
        );

        repository.save(resource);

        return new DisassociateTagResponse();
    }

    @Override
    @ValidateRequest
    @Transactional
    public UpdateDescriptionResponse updateDescription(UpdateDescriptionCmd cmd) {
        Resource resource = repository.findResource(cmd.getResourceId());

        resource.setDescription(cmd.getDescription());

        AuditLogContext.current().addAuditObject(
                new AuditObject(resource.getId().toString(), resource.getName())
        );

        repository.save(resource);

        return new UpdateDescriptionResponse();
    }

    private void synchronizeResourceStatesByAccount(ExternalAccount account) {
        List<? extends ResourceHandler> resourceHandlers = account.getProvider().getResourceHandlers();

        if(Utils.isEmpty(resourceHandlers))
            return;



        List<Resource> synchronizedResources = new ArrayList<>();
        for (ResourceHandler resourceHandler : resourceHandlers) {
            try {
                ResourceFilters resourceFilters = ResourceFilters.builder()
                        .accountIds(List.of(account.getId()))
                        .resourceTypes(List.of(resourceHandler.getResourceTypeId()))
                        .build();

                var resourcesMap = repository.findAllByFilters(resourceFilters).stream().filter(
                        r -> Utils.isNotBlank(r.getExternalId())
                ).collect(
                        Collectors.toMap(Resource::getExternalId, r -> r)
                );


                List<ExternalResource> externalResources = resourceHandler.describeExternalResources(account);

                if(Utils.isEmpty(externalResources))
                    continue;

                log.info("Synchronizing {} resources states of type {} of account {}.",
                        externalResources.size(), resourceHandler.getResourceTypeId(), account.getName());

                for (ExternalResource externalResource : externalResources) {
                    Resource resource = resourcesMap.get(externalResource.externalId());

                    if(resource != null) {
                        resource.updateByExternal(externalResource);
                        synchronizedResources.add(resource);
                    }
                }
            }catch (Exception e){
                log.warn("Failed to sync resource states of type {} of account {}.",
                        resourceHandler.getResourceTypeId(), account.getName(), e);
            }
        }

        repository.saveAll(synchronizedResources);

        log.info("{} resources states synchronized of account {}.",
                synchronizedResources.size(), account.getName());
    }
}
