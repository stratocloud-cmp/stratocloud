package com.stratocloud.kubernetes.volume;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPodVolumeHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesPodVolumeHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_POD_VOLUME";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s PodVolume";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.POD_VOLUME;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describePodVolume(account, externalId).map(
                p -> toExternalResource(account, p)
        );
    }

    public Optional<PodVolume> describePodVolume(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describePodVolume(PodVolumeId.fromString(externalId));
    }

    public ExternalResource toExternalResource(ExternalAccount account,
                                                PodVolume podVolume) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                podVolume.id().toString(),
                podVolume.id().volumeName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account,
                                                            Map<String, Object> queryArgs) {
        return provider.buildClient(account).describePodVolumes().stream().map(
                v -> toExternalResource(account, v)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource externalResource = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Volume not found")
        );
        resource.updateByExternal(externalResource);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
