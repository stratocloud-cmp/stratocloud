package com.stratocloud.kubernetes.daemon;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.KubernetesManagementService;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesDaemonSetHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    private final KubernetesManagementService managementService;

    public KubernetesDaemonSetHandler(KubernetesProvider provider,
                                      KubernetesManagementService managementService) {
        this.provider = provider;
        this.managementService = managementService;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_DAEMON_SET";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s DaemonSet";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.DAEMON_SET;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeDaemonSet(account, externalId).map(
                d -> toExternalResource(account, d)
        );
    }

    public Optional<V1DaemonSet> describeDaemonSet(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeDaemonSet(NamespacedRef.fromString(externalId));
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1DaemonSet daemonSet) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getNamespacedRef(daemonSet.getMetadata()).toString(),
                KubeUtil.getObjectName(daemonSet.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeDaemonSets().stream().map(
                d -> toExternalResource(account, d)
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

    public void managePodsAndVolumes(Resource resource){
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1DaemonSet> daemonSet = describeDaemonSet(
                account, resource.getExternalId()
        );

        if(daemonSet.isEmpty())
            return;

        V1ObjectMeta metadata = daemonSet.get().getMetadata();

        if(metadata == null)
            return;

        managementService.managePodsAndVolumes(
                provider,
                account,
                daemonSet.get().getKind(),
                metadata,
                resource.getOwnerId()
        );
    }
}
