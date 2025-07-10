package com.stratocloud.kubernetes.pod.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.KubernetesClient;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.deployment.KubernetesDeploymentHandler;
import com.stratocloud.kubernetes.pod.KubernetesPodHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPodToDeploymentHandler implements ExclusiveRequirementHandler {

    private final KubernetesPodHandler podHandler;

    private final KubernetesDeploymentHandler deploymentHandler;

    public KubernetesPodToDeploymentHandler(KubernetesPodHandler podHandler,
                                            KubernetesDeploymentHandler deploymentHandler) {
        this.podHandler = podHandler;
        this.deploymentHandler = deploymentHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_POD_TO_DEPLOYMENT_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Deployment与Pod";
    }

    @Override
    public ResourceHandler getSource() {
        return podHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return deploymentHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Pod";
    }

    @Override
    public String getRequirementName() {
        return "Deployment";
    }

    @Override
    public String getConnectActionName() {
        return "关联";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }

    @Override
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public RelationshipActionResult checkConnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public boolean visibleInForm() {
        return false;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<V1Pod> pod = podHandler.describePod(account, source.externalId());

        if(pod.isEmpty())
            return List.of();

        Optional<V1OwnerReference> ownerReference = KubeUtil.getOwnerReference(
                pod.get().getMetadata(), "ReplicaSet"
        );

        if(ownerReference.isEmpty())
            return List.of();

        KubernetesProvider provider = (KubernetesProvider) podHandler.getProvider();
        KubernetesClient client = provider.buildClient(account);

        NamespacedRef podRef = NamespacedRef.fromString(source.externalId());

        Optional<V1ReplicaSet> replicaSet = client.describeReplicaSet(
                new NamespacedRef(podRef.namespace(), ownerReference.get().getName())
        );

        if(replicaSet.isEmpty())
            return List.of();

        Optional<V1OwnerReference> deploymentRef = KubeUtil.getOwnerReference(
                replicaSet.get().getMetadata(), "Deployment"
        );

        if(deploymentRef.isEmpty())
            return List.of();

        Optional<ExternalResource> deployment = deploymentHandler.describeExternalResource(
                account,
                new NamespacedRef(
                        podRef.namespace(),
                        deploymentRef.get().getName()
                ).toString()
        );

        return deployment.map(er -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        er,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
