package com.stratocloud.kubernetes.deployment;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.KubernetesManagementService;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.event.EventAwareResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class KubernetesDeploymentHandler extends AbstractResourceHandler implements EventAwareResourceHandler {

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

        List<V1DeploymentCondition> conditions = status.getConditions();

        if(Utils.isEmpty(conditions))
            return ResourceState.UNKNOWN;

        Optional<V1DeploymentCondition> availableCondition = conditions.stream().filter(
                c -> "Available".equals(c.getType())
        ).findFirst();

        if(availableCondition.isPresent() && "True".equals(availableCondition.get().getStatus())) {
            int replicas = status.getReplicas() != null ? status.getReplicas() : 0;

            return replicas > 0 ? ResourceState.STARTED : ResourceState.STOPPED;
        }

        Optional<V1DeploymentCondition> progressingCondition = conditions.stream().filter(
                c -> "Progressing".equals(c.getType())
        ).findFirst();

        if(progressingCondition.isPresent()){
            if("True".equals(progressingCondition.get().getStatus())) {
                return ResourceState.STARTING;
            }else{
                log.warn("Deployment's progressing condition failure reason: {}. Deployment={}.",
                        progressingCondition.get().getReason(), KubeUtil.getObjectName(deployment.getMetadata()));
                return ResourceState.ERROR;
            }
        } else {
            log.warn("Deployment's progressing condition not found. Deployment={}.",
                    KubeUtil.getObjectName(deployment.getMetadata()));
            return ResourceState.ERROR;
        }
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
        ExternalResource externalResource = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Deployment not found")
        );
        resource.updateByExternal(externalResource);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
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

        List<V1ReplicaSet> replicaSets = provider.buildClient(account).describeReplicaSetsByNamespace(
                metadata.getNamespace()
        ).stream().filter(
                rs -> KubeUtil.getOwnerReference(
                        rs.getMetadata(), "Deployment"
                ).filter(
                        ref -> Objects.equals(ref.getName(), metadata.getName())
                ).isPresent()
        ).toList();

        if(Utils.isEmpty(replicaSets))
            return;

        for (V1ReplicaSet replicaSet : replicaSets) {
            if(replicaSet.getMetadata() == null)
                continue;

            managementService.managePodsAndVolumes(
                    provider,
                    account,
                    "ReplicaSet",
                    replicaSet.getMetadata(),
                    resource.getOwnerId()
            );
        }


    }

    @Override
    public List<ExternalResourceEvent> describeResourceEvents(ExternalAccount account,
                                                              String externalId,
                                                              LocalDateTime happenedAfter) {
        return KubeUtil.describeResourceEvents(
                provider,
                account,
                "Deployment",
                getResourceTypeId(),
                externalId,
                happenedAfter,
                true
        );
    }
}
