package com.stratocloud.provider;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.auth.CallContext;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.provider.dynamic.DynamicResourceHandlerLoader;
import com.stratocloud.provider.relationship.EssentialPrimaryCapabilityHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public abstract class AbstractResourceHandler implements ResourceHandler {
    private final List<ResourceActionHandler> actionHandlers = new ArrayList<>();

    private final List<ResourceReadActionHandler> readActionHandlers = new ArrayList<>();

    private final List<RelationshipHandler> requirements = new ArrayList<>();

    private final List<RelationshipHandler> capabilities = new ArrayList<>();

    @Override
    public List<? extends ResourceActionHandler> getActionHandlers() {
        return actionHandlers;
    }

    @Override
    public List<RelationshipHandler> getRequirements() {
        return requirements;
    }

    @Override
    public List<RelationshipHandler> getCapabilities() {
        Set<RelationshipHandler> result = new HashSet<>(capabilities);

        List<DynamicResourceHandlerLoader> loaders = getProvider().getResourceHandlerLoaders();

        for (DynamicResourceHandlerLoader loader : loaders) {
            try {
                result.addAll(loader.loadCapabilitiesByTarget(this));
            }catch (Exception e){
                log.warn("Failed to load capabilities from {}: {}.", loader.getClass().getSimpleName(), e.toString());
            }
        }

        return List.copyOf(result);
    }

    @Override
    public Optional<ResourceActionHandler> getActionHandler(ResourceAction action){
        return getActionHandler(action.id());
    }

    @Override
    public Optional<ResourceActionHandler> getActionHandler(String actionId) {
        return actionHandlers.stream().filter(
                rah -> rah.getAction().id().equals(actionId)
        ).findAny();
    }

    @Override
    public void registerReadActionHandler(ResourceReadActionHandler readActionHandler) {
        readActionHandlers.add(readActionHandler);
    }

    @Override
    public List<? extends ResourceReadActionHandler> getReadActionHandlers() {
        return readActionHandlers;
    }

    @Override
    public Optional<ResourceReadActionHandler> getReadActionHandler(String actionId) {
        return readActionHandlers.stream().filter(h -> Objects.equals(actionId, h.getAction().id())).findAny();
    }

    @Override
    public Set<ResourceAction> getAvailableReadActions(Resource resource) {
        Set<ResourceAction> actionSet = new HashSet<>();

        for (ResourceReadActionHandler readActionHandler : getReadActionHandlers()) {
            PermissionItem permissionItem = readActionHandler.getPermissionItem();
            if(readActionHandler.getAllowedStates().contains(resource.getState()))
                if(CallContext.current().hasPermission(permissionItem.target(), permissionItem.action()))
                    actionSet.add(readActionHandler.getAction());
        }

        return actionSet;
    }

    @Override
    public RelationshipHandler getCapability(String relationshipTypeId){
        return getCapabilities().stream().filter(
                c -> c.getRelationshipTypeId().equals(relationshipTypeId)
        ).findAny().orElseThrow(()->new StratoException("Unknown capability type: " + relationshipTypeId));
    }

    @Override
    public RelationshipHandler getRequirement(String relationshipTypeId){
        return getRequirements().stream().filter(
                r -> r.getRelationshipTypeId().equals(relationshipTypeId)
        ).findAny().orElseThrow(()->new StratoException("Unknown requirement type: " + relationshipTypeId));
    }

    @Override
    public synchronized void registerActionHandler(ResourceActionHandler resourceActionHandler) {
        actionHandlers.add(resourceActionHandler);
    }

    @Override
    public synchronized void registerRequirement(RelationshipHandler relationshipHandler) {
        requirements.add(relationshipHandler);
    }

    @Override
    public synchronized void registerCapability(RelationshipHandler relationshipHandler) {
        capabilities.add(relationshipHandler);
    }

    @Override
    public void runAction(Resource resource, ResourceAction action, Map<String, Object> parameters) {
        validateActionPrecondition(resource, action.id(), parameters);

        ResourceActionHandler actionHandler = getHandler(action);

        actionHandler.run(resource, parameters);

        actionHandler.getTransitionState().ifPresent(resource::setState);
    }

    @Override
    public void validateActionPrecondition(Resource resource, String actionId, Map<String, Object> parameters) {
        if(ResourceActions.DESTROY_RESOURCE.id().equals(actionId)) {
            if (!supportCascadedDestruction()) {
                List<Relationship> aliveCapabilities = resource.getCapabilities().stream().filter(
                        rel -> ResourceState.getAliveStateSet().contains(rel.getSource().getState())
                ).filter(
                        rel -> ! (rel.getHandler() instanceof EssentialPrimaryCapabilityHandler)
                ).filter(
                        rel -> rel.getState() != RelationshipState.DISCONNECTED
                ).toList();
                if (!aliveCapabilities.isEmpty())
                    throw new BadCommandException(
                            "Resource %s is still required by %s capabilities, recycle these capabilities first."
                                    .formatted(resource.getName(), aliveCapabilities.size())
                    );
            }
        }




        ResourceActionHandler actionHandler = getHandler(actionId);

        actionHandler.validatePermission();

        Set<ResourceState> allowedStates = actionHandler.getAllowedStates();

        if(!allowedStates.contains(resource.getState()))
            throw new BadCommandException("Cannot run resource action %s from state %s. ResourceType: %s.".formatted(
                    actionId, resource.getState(), getResourceTypeId()
            ));

        actionHandler.validatePrecondition(resource, parameters);
    }

    @Override
    public Set<ResourceAction> getAvailableActions(Resource resource){
        Set<ResourceAction> actionSet = new HashSet<>();

        for (ResourceActionHandler actionHandler : actionHandlers) {
            PermissionItem permissionItem = actionHandler.getPermissionItem();
            if(actionHandler.getAllowedStates().contains(resource.getState()))
                if(CallContext.current().hasPermission(permissionItem.target(), permissionItem.action()))
                    actionSet.add(actionHandler.getAction());
        }

        return actionSet;
    }


    private ResourceActionHandler getHandler(ResourceAction action) {
        return getHandler(action.id());
    }

    private ResourceActionHandler getHandler(String actionId) {
        String notFoundMessage = "ResourceActionHandler not found by action: %s. ResourceType: %s.".formatted(
                actionId, getResourceTypeId()
        );
        return getActionHandler(actionId).orElseThrow(() -> new StratoException(notFoundMessage));
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource,
                                                  ResourceAction action,
                                                  Map<String, Object> parameters) {
        ResourceActionHandler actionHandler = getHandler(action);

        return actionHandler.checkActionResult(resource, parameters);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource externalResource) {
        List<ExternalRequirement> result = new ArrayList<>();

        for (RelationshipHandler handler : getRequirements()) {
            result.addAll(handler.describeExternalRequirements(account, externalResource));
        }

        return result;
    }
}
