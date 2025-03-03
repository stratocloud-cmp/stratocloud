package com.stratocloud.provider.tencent.lb.backend;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentInstanceBackendHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentInstanceBackendHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_INSTANCE_BACKEND";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云云主机后端服务";
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
        Optional<TencentBackend> backend = describeBackend(account, externalId);
        return backend.map(b -> toExternalResource(account, b));
    }

    public Optional<TencentBackend> describeBackend(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();
        TencentInstanceBackendId backendId = TencentInstanceBackendId.fromString(externalId);
        return provider.buildClient(account).describeBackend(backendId);
    }

    private ExternalResource toExternalResource(ExternalAccount account,
                                                TencentBackend backend) {
        TencentInstanceBackendId backendId = new TencentInstanceBackendId(
                backend.lbId(), backend.listenerId(), backend.backend().getInstanceId()
        );
        return new ExternalResource(
                account.getProviderId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                backendId.toString(),
                backend.backend().getInstanceName(),
                ResourceState.IN_USE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        List<TencentBackend> backends = provider.buildClient(account).describeBackends().stream().filter(
                backend -> Objects.equals(backend.backend().getType(), "CVM")
        ).toList();

        return backends.stream().map(backend -> toExternalResource(account, backend)).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource backend = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Backend not found")
        );
        resource.updateByExternal(backend);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
