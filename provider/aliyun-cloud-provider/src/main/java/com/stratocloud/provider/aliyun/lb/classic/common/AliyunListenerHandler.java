package com.stratocloud.provider.aliyun.lb.classic.common;

import com.aliyun.slb20140515.models.DescribeLoadBalancerListenersRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AliyunListenerHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    protected AliyunListenerHandler(AliyunCloudProvider provider) {
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
        Optional<AliyunListener> listener = describeListener(account, externalId);
        return listener.map(lbl -> toExternalResource(account, lbl));
    }

    public Optional<AliyunListener> describeListener(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        AliyunListenerId listenerId = AliyunListenerId.fromString(externalId);

        return provider.buildClient(account).clb().describeListener(listenerId).filter(this::listenerFilter);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunListener listener) {
        String protocol = listener.detail().getListenerProtocol();
        Integer port = listener.detail().getListenerPort();

        String listenerId = new AliyunListenerId(
                listener.detail().getLoadBalancerId(),
                protocol,
                port.toString()
        ).toString();

        return new ExternalResource(
                account.getProviderId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                listenerId,
                listenerId,
                convertStatus(listener.detail().getStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        return switch (status){
            case "running" -> ResourceState.STARTED;
            case "starting" -> ResourceState.STARTING;
            case "stopped" -> ResourceState.STOPPED;
            case "stopping" -> ResourceState.STOPPING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        DescribeLoadBalancerListenersRequest request = new DescribeLoadBalancerListenersRequest();

        List<AliyunListener> listeners = provider.buildClient(account).clb().describeListeners(
                request
        ).stream().filter(
                this::listenerFilter
        ).toList();

        return listeners.stream().map(lbl -> toExternalResource(account, lbl)).toList();
    }

    protected abstract boolean listenerFilter(AliyunListener listener);

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunListener listener = describeListener(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Listener not found.")
        );

        resource.updateByExternal(toExternalResource(account, listener));

        String port = String.valueOf(listener.detail().getListenerPort());
        RuntimeProperty portProperty = RuntimeProperty.ofDisplayInList(
                "port", "端口", port, port
        );
        resource.addOrUpdateRuntimeProperty(portProperty);

        String protocol = listener.detail().getListenerProtocol();
        RuntimeProperty protocolProperty = RuntimeProperty.ofDisplayInList(
                "protocol", "协议", protocol, protocol
        );
        resource.addOrUpdateRuntimeProperty(protocolProperty);

        String scheduler = listener.detail().getScheduler();
        RuntimeProperty schedulerProperty = RuntimeProperty.ofDisplayInList(
                "scheduler", "调度算法", scheduler, scheduler
        );
        resource.addOrUpdateRuntimeProperty(schedulerProperty);

        Integer backendServerPort = listener.detail().getBackendServerPort();
        if(backendServerPort != null){
            RuntimeProperty backendPortProperty = RuntimeProperty.ofDisplayInList(
                    "backendPort", "后端服务器端口", backendServerPort.toString(), backendServerPort.toString()
            );
            resource.addOrUpdateRuntimeProperty(backendPortProperty);
        }

        Integer bandwidth = listener.detail().getBandwidth();
        if(bandwidth != null){
            RuntimeProperty bandwidthProperty = RuntimeProperty.ofDisplayInList(
                    "bandwidth", "带宽峰值(Mbps)", bandwidth.toString(), bandwidth.toString()
            );
            resource.addOrUpdateRuntimeProperty(bandwidthProperty);
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
