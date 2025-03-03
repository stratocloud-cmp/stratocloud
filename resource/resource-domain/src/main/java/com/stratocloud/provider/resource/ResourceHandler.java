package com.stratocloud.provider.resource;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.utils.JSON;

import java.util.*;

public interface ResourceHandler extends DynamicPermissionRequired {
    Provider getProvider();
    String getResourceTypeId();
    String getResourceTypeName();
    ResourceCategory getResourceCategory();

    default boolean isManageable(){
        return true;
    }

    boolean isInfrastructure();

    default boolean isSharedRequirementTarget(){
        return false;
    }

    default boolean supportCascadedDestruction(){
        return false;
    }

    void registerActionHandler(ResourceActionHandler resourceActionHandler);
    List<? extends ResourceActionHandler> getActionHandlers();

    void registerReadActionHandler(ResourceReadActionHandler readActionHandler);
    Set<ResourceAction> getAvailableReadActions(Resource resource);
    List<? extends ResourceReadActionHandler> getReadActionHandlers();
    Optional<ResourceReadActionHandler> getReadActionHandler(String actionId);

    void registerRequirement(RelationshipHandler relationshipHandler);

    void registerCapability(RelationshipHandler relationshipHandler);

    List<? extends RelationshipHandler> getCapabilities();

    default List<ResourceHandler> getCapabilitiesSources(){
        return getCapabilities().stream().map(RelationshipHandler::getSource).toList();
    }

    List<? extends RelationshipHandler> getRequirements();

    default List<ResourceHandler> getRequirementsTargets(){
        return getRequirements().stream().map(RelationshipHandler::getTarget).toList();
    }

    Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId);

    List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs);

    default List<ExternalResource> describeExternalResources(ExternalAccount account){
        return describeExternalResources(account, new HashMap<>());
    }

    List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource externalResource);

    default List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource){
        return new ArrayList<>();
    }

    void synchronize(Resource resource);


    default Map<String, Object> getPropertiesAtIndex(Map<String, Object> properties, int index){
        return JSON.clone(properties);
    }


    void runAction(Resource resource, ResourceAction action, Map<String, Object> parameters);

    void validateActionPrecondition(Resource resource, String actionId, Map<String, Object> parameters);

    Set<ResourceAction> getAvailableActions(Resource resource);

    ResourceActionResult checkActionResult(Resource resource, ResourceAction action, Map<String, Object> parameters);

    Optional<ResourceActionHandler> getActionHandler(ResourceAction action);

    Optional<ResourceActionHandler> getActionHandler(String actionId);

    RelationshipHandler getCapability(String relationshipTypeId);

    RelationshipHandler getRequirement(String relationshipTypeId);

    List<ResourceUsageType> getUsagesTypes();

    default ResourceUsageType getUsageType(String typeId){
        return getUsagesTypes().stream().filter(
                ut -> Objects.equals(typeId, ut.type())
        ).findAny().orElseThrow(
                () -> new StratoException("ResourceUsageType not found: %s".formatted(typeId))
        );
    }

    default boolean canAttachIpPool(){
        return false;
    }

    default boolean canConnectTo(ResourceHandler resourceHandler){
        return getRequirementsTargets().contains(resourceHandler);
    }

    default ExternalAccountRepository getAccountRepository(){
        return getProvider().getAccountRepository();
    }

    @Override
    default PermissionItem getPermissionItem() {
        return new PermissionItem(
                getResourceCategory().id(),
                getResourceCategory().name(),
                "READ",
                "查询"
        );
    }

    default ResourceCost getCurrentCost(Resource resource){
        return ResourceCost.ZERO;
    }
}
