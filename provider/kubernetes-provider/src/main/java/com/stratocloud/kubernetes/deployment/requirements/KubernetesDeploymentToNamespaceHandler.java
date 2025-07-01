package com.stratocloud.kubernetes.deployment.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.deployment.KubernetesDeploymentHandler;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesDeploymentToNamespaceHandler implements EssentialRequirementHandler {

    private final KubernetesDeploymentHandler deploymentHandler;

    private final KubernetesNamespaceHandler namespaceHandler;

    public KubernetesDeploymentToNamespaceHandler(KubernetesDeploymentHandler deploymentHandler,
                                                  KubernetesNamespaceHandler namespaceHandler) {
        this.deploymentHandler = deploymentHandler;
        this.namespaceHandler = namespaceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_DEPLOYMENT_TO_NAMESPACE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Deployment与Namespace";
    }

    @Override
    public ResourceHandler getSource() {
        return deploymentHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return namespaceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Deployment";
    }

    @Override
    public String getRequirementName() {
        return "Namespace";
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        NamespacedRef deploymentRef = NamespacedRef.fromString(source.externalId());

        Optional<ExternalResource> namespace = namespaceHandler.describeExternalResource(
                account, deploymentRef.namespace()
        );

        return namespace.map(
                n -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                n,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
