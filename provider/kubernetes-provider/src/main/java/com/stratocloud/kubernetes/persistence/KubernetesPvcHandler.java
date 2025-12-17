package com.stratocloud.kubernetes.persistence;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPvcHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesPvcHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_PERSISTENT_VOLUME_CLAIM";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s PVC";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.CONTAINER_STORAGE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describePersistentVolumeClaim(account, externalId).map(
                i -> toExternalResource(account, i)
        );
    }

    public Optional<V1PersistentVolumeClaim> describePersistentVolumeClaim(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describePersistentVolumeClaim(NamespacedRef.fromString(externalId));
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1PersistentVolumeClaim volumeClaim) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getObjectName(volumeClaim.getMetadata()),
                KubeUtil.getObjectName(volumeClaim.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describePersistentVolumeClaims().stream().map(
                p -> toExternalResource(account, p)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        V1PersistentVolumeClaim pvc = describePersistentVolumeClaim(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("PVC not found")
        );
        resource.updateByExternal(toExternalResource(account, pvc));

        V1PersistentVolumeClaimStatus status = pvc.getStatus();

        if(status != null){
            Map<String, Quantity> capacity = status.getCapacity();

            if(capacity != null){
                Quantity quantity = capacity.get("storage");

                if(quantity != null){
                    String storageSize = quantity.getNumber().divide(
                            BigDecimal.valueOf(2L).pow(30), RoundingMode.FLOOR
                    ).setScale(
                            2, RoundingMode.FLOOR
                    ).toPlainString();

                    RuntimeProperty storageSizeProperty = RuntimeProperty.ofDisplayInList(
                            "storageSize",
                            "存储容量(GiB)",
                            storageSize,
                            storageSize
                    );

                    resource.addOrUpdateRuntimeProperty(storageSizeProperty);
                }
            }
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
