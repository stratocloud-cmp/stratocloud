package com.stratocloud.kubernetes.runtime;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1RuntimeClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesRuntimeClassHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesRuntimeClassHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_RUNTIME_CLASS";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s RuntimeClass";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.RUNTIME_CLASS;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeRuntimeClass(account, externalId).map(
                r -> toExternalResource(account, r)
        );
    }

    public Optional<V1RuntimeClass> describeRuntimeClass(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeRuntimeClass(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1RuntimeClass runtimeClass) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getObjectName(runtimeClass.getMetadata()),
                KubeUtil.getObjectName(runtimeClass.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeRuntimeClasses().stream().map(
                r -> toExternalResource(account, r)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> externalResource = describeExternalResource(account, resource.getExternalId());
        externalResource.ifPresent(resource::updateByExternal);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
