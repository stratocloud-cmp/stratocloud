package com.stratocloud.provider.huawei.elb.listener;

import com.huaweicloud.sdk.elb.v3.model.ListListenersRequest;
import com.huaweicloud.sdk.elb.v3.model.Listener;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.HuaweiLbStatusTreeHelper;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiListenerHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiListenerHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_ELB_LISTENER";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云ELB监听器";
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
        Optional<Listener> listener = describeListener(account, externalId);
        return listener.map(l -> toExternalResource(account, l));
    }

    public Optional<Listener> describeListener(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).elb().describeListener(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, Listener listener) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                listener.getId(),
                listener.getName(),
                HuaweiLbStatusTreeHelper.getListenerState(provider, account.getId(), listener)
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        List<Listener> listeners = provider.buildClient(account).elb().describeListeners(
                new ListListenersRequest()
        );
        return listeners.stream().map(l -> toExternalResource(account, l)).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Listener listener = describeListener(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Listener not found.")
        );
        resource.updateByExternal(toExternalResource(account, listener));

        RuntimeProperty protocolProperty = RuntimeProperty.ofDisplayInList(
                "protocol",
                "监听器协议",
                listener.getProtocol(),
                listener.getProtocol()
        );
        resource.addOrUpdateRuntimeProperty(protocolProperty);

        RuntimeProperty portProperty = RuntimeProperty.ofDisplayInList(
                "port",
                "监听端口",
                listener.getProtocolPort().toString(),
                listener.getProtocolPort().toString()
        );
        resource.addOrUpdateRuntimeProperty(portProperty);

        try {
            HuaweiLbStatusTreeHelper.synchronizeListenerStatusTree(resource);
        }catch (Exception e){
            log.warn("Failed to synchronize LB listener status: {}.", e.toString());
        }
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
