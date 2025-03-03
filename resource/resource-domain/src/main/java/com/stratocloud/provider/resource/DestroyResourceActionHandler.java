package com.stratocloud.provider.resource;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface DestroyResourceActionHandler extends ResourceActionHandler {

    @Override
    default ResourceAction getAction() {
        return ResourceActions.DESTROY_RESOURCE;
    }

    @Override
    default Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.values());
    }

    @Override
    default Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.DESTROYING);
    }

    @Override
    default ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        if(Utils.isBlank(resource.getExternalId())) {
            resource.onDestroyed();
            return ResourceActionResult.finished();
        }

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var externalResource = getResourceHandler().describeExternalResource(account, resource.getExternalId());

        if(externalResource.isEmpty() || externalResource.get().state() == ResourceState.DESTROYED) {
            resource.onDestroyed();
            return ResourceActionResult.finished();
        }

        if(externalResource.get().state() == ResourceState.DESTROYING)
            return ResourceActionResult.inProgress();

        if(externalResource.get().state() == ResourceState.BUILD_ERROR)
            return ResourceActionResult.finished();

        return ResourceActionResult.failed("Resource %s is still alive.".formatted(resource.getName()));
    }

    @Override
    default List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters){
        return List.of();
    }

    @Override
    default void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
