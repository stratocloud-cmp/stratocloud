package com.stratocloud.provider.resource;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BuildResourceActionHandler extends ResourceActionHandler {

    @Override
    default ResourceAction getAction() {
        return ResourceActions.BUILD_RESOURCE;
    }

    @Override
    default Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.NO_STATE);
    }

    @Override
    default Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.BUILDING);
    }

    @Override
    default ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        if(Utils.isBlank(resource.getExternalId())) {
            resource.setState(ResourceState.BUILD_ERROR);
            return ResourceActionResult.failed("Failed to create resource: " + resource.getName());
        }

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var externalResource = getResourceHandler().describeExternalResource(account, resource.getExternalId());

        if(externalResource.isEmpty()) {
            resource.setState(ResourceState.BUILD_ERROR);
            return ResourceActionResult.failed("Failed to create resource: " + resource.getName());
        }

        if(externalResource.get().state() == ResourceState.BUILDING)
            return ResourceActionResult.inProgress();

        return ResourceActionResult.finished();
    }
}
