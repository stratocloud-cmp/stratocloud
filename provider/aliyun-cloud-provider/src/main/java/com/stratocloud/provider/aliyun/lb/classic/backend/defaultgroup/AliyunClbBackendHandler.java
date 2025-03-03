package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup;

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

public abstract class AliyunClbBackendHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunClbBackendHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LOAD_BALANCER_BACKEND;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<AliyunClbBackend> backend = describeBackend(account, externalId);

        return backend.map(
                value -> toExternalResource(account, value)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunClbBackend backend) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                backend.id().toString(),
                backend.name(),
                convertStatus(backend.health().getServerHealthStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        return switch (status) {
            case "normal" -> ResourceState.HEALTH_CHECK_NORMAL;
            case "abnormal" -> ResourceState.HEALTH_CHECK_ABNORMAL;
            case "unavailable" -> ResourceState.HEALTH_CHECK_UNAVAILABLE;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<AliyunClbBackend> describeBackend(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        AliyunClbBackendId backendId = AliyunClbBackendId.fromString(externalId);

        return provider.buildClient(account).clb().describeBackend(backendId).filter(this::filterBackend);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).clb().describeBackends().stream().filter(
                this::filterBackend
        ).map(
                clb -> toExternalResource(account, clb)
        ).toList();
    }

    protected abstract boolean filterBackend(AliyunClbBackend backend);

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClbBackend backend = describeBackend(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Backend not found.")
        );

        resource.updateByExternal(toExternalResource(account, backend));

        String weight = backend.detail().getWeight().toString();
        RuntimeProperty weightProperty = RuntimeProperty.ofDisplayInList(
                "weight", "权重", weight, weight
        );

        String listenerPort = backend.health().getListenerPort().toString();

        RuntimeProperty listenerPortProperty = RuntimeProperty.ofDisplayable(
                "listenerPort", "监听器端口", listenerPort, listenerPort
        );

        resource.addOrUpdateRuntimeProperty(weightProperty);
        resource.addOrUpdateRuntimeProperty(listenerPortProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
