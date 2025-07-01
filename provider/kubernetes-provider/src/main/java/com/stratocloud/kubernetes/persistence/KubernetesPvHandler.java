package com.stratocloud.kubernetes.persistence;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPvHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesPvHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_PERSISTENT_VOLUME";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s PersistentVolume";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.PERSISTENT_VOLUME;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describePersistentVolume(account, externalId).map(
                p -> toExternalResource(account, p)
        );
    }

    public Optional<V1PersistentVolume> describePersistentVolume(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describePersistentVolume(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account,
                                                V1PersistentVolume persistentVolume) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getObjectName(persistentVolume.getMetadata()),
                KubeUtil.getObjectName(persistentVolume.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account,
                                                            Map<String, Object> queryArgs) {
        return provider.buildClient(account).describePersistentVolumes().stream().map(
                p -> toExternalResource(account, p)
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
