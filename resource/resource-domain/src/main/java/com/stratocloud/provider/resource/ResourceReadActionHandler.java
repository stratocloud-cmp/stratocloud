package com.stratocloud.provider.resource;


import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceAction;
import com.stratocloud.resource.ResourceReadActionResult;
import com.stratocloud.resource.ResourceState;

import java.util.List;
import java.util.Set;

public interface ResourceReadActionHandler extends DynamicPermissionRequired {
    ResourceHandler getResourceHandler();

    ResourceAction getAction();

    Set<ResourceState> getAllowedStates();



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

    List<ResourceReadActionResult> performReadAction(Resource resource);
}
