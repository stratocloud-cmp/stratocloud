package com.stratocloud.kubernetes.deployment;

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
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesDeploymentHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    private final KubernetesManagementService managementService;

    public KubernetesDeploymentHandler(KubernetesProvider provider,
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
        return "KUBERNETES_DEPLOYMENT";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s Deployment";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.DEPLOYMENT;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeDeployment(account, externalId).map(
                n -> toExternalResource(account, n)
        );
    }

    public Optional<V1Deployment> describeDeployment(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeDeployment(NamespacedRef.fromString(externalId));
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1Deployment deployment) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getNamespacedRef(deployment.getMetadata()).toString(),
                KubeUtil.getObjectName(deployment.getMetadata()),
                convertState(deployment)
        );
    }

    private ResourceState convertState(V1Deployment deployment) {
        V1DeploymentStatus status = deployment.getStatus();

        if(status == null)
            return ResourceState.UNKNOWN;

        Integer replicas = status.getReplicas();
        Integer availableReplicas = status.getAvailableReplicas();

        if(replicas == null || availableReplicas == null)
            return ResourceState.UNKNOWN;

        if(replicas > availableReplicas)
            return ResourceState.BUILDING;

        if(replicas == 0)
            return ResourceState.STOPPED;

        return ResourceState.STARTED;
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeDeployments().stream().map(
                n -> toExternalResource(account, n)
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

        Optional<V1Deployment> deployment = describeDeployment(
                account, resource.getExternalId()
        );

        if(deployment.isEmpty())
            return;

        V1ObjectMeta metadata = deployment.get().getMetadata();

        if(metadata == null)
            return;

        managementService.managePodsAndVolumes(
                provider,
                account,
                deployment.get().getKind(),
                metadata,
                resource.getOwnerId()
        );
    }
}
