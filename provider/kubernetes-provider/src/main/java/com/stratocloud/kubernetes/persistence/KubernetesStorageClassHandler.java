package com.stratocloud.kubernetes.persistence;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1StorageClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesStorageClassHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesStorageClassHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_STORAGE_CLASS";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s StorageClass";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.CLUSTER_CONFIG;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeStorageClass(account, externalId).map(
                s -> toExternalResource(account, s)
        );
    }

    public Optional<V1StorageClass> describeStorageClass(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeStorageClass(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1StorageClass storageClass) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getObjectName(storageClass.getMetadata()),
                KubeUtil.getObjectName(storageClass.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeStorageClasses().stream().map(
                s -> toExternalResource(account, s)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource externalResource = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("StorageClass not found")
        );
        resource.updateByExternal(externalResource);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
