package com.stratocloud.provider.tencent.lb.common;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.listener.TencentListener;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class TencentListenerHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentListenerHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }


    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LOAD_BALANCER_LISTENER;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<TencentListener> listener = describeListener(account, externalId);
        return listener.map(lbl -> toExternalResource(account, lbl));
    }

    public Optional<TencentListener> describeListener(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentListenerId listenerId = TencentListenerId.fromString(externalId);

        return provider.buildClient(account).describeListener(listenerId).filter(this::listenerFilter);
    }

    private ExternalResource toExternalResource(ExternalAccount account, TencentListener listener) {
        return new ExternalResource(
                account.getProviderId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                new TencentListenerId(
                        listener.loadBalancerId(),
                        listener.listener().getListenerId()
                ).toString(),
                listener.listener().getListenerName(),
                ResourceState.STARTED
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        List<TencentListener> listeners = provider.buildClient(account).describeListeners().stream().filter(
                this::listenerFilter
        ).toList();

        return listeners.stream().map(lbl -> toExternalResource(account, lbl)).toList();
    }

    protected abstract boolean listenerFilter(TencentListener listener);

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource externalResource = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Listener not found.")
        );
        resource.updateByExternal(externalResource);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }
}
