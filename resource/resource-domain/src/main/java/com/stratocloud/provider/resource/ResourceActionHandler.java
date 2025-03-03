package com.stratocloud.provider.resource;


import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.resource.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ResourceActionHandler extends DynamicPermissionRequired {
    ResourceHandler getResourceHandler();

    ResourceAction getAction();

    String getTaskName();

    Set<ResourceState> getAllowedStates();

    Optional<ResourceState> getTransitionState();

    Class<? extends ResourceActionInput> getInputClass();

    default Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(){
        return Optional.empty();
    }

    void run(Resource resource, Map<String, Object> parameters);

    ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters);

    List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters);

    void validatePrecondition(Resource resource, Map<String, Object> parameters);

    @Override
    default PermissionItem getPermissionItem() {
        return new PermissionItem(
                getResourceHandler().getResourceCategory().id(),
                getResourceHandler().getResourceCategory().name(),
                getAction().id(),
                getAction().name()
        );
    }

    default ExternalAccountRepository getAccountRepository(){
        return getResourceHandler().getAccountRepository();
    }


    default ResourceCost getActionCost(Resource resource, Map<String, Object> parameters){
        return ResourceCost.ZERO;
    }

    default List<String> getLockExclusiveTargetRelTypeIds(){
        return List.of();
    }

    default int getLockExclusiveTargetMaxSeconds(){
        return 60;
    }
}
