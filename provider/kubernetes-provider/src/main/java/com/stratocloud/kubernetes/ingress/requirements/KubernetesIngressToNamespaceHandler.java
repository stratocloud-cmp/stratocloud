package com.stratocloud.kubernetes.ingress.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.ingress.KubernetesIngressHandler;
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
public class KubernetesIngressToNamespaceHandler implements EssentialRequirementHandler {

    private final KubernetesIngressHandler ingressHandler;

    private final KubernetesNamespaceHandler namespaceHandler;

    public KubernetesIngressToNamespaceHandler(KubernetesIngressHandler ingressHandler,
                                               KubernetesNamespaceHandler namespaceHandler) {
        this.ingressHandler = ingressHandler;
        this.namespaceHandler = namespaceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_INGRESS_TO_NAMESPACE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Ingress与Namespace";
    }

    @Override
    public ResourceHandler getSource() {
        return ingressHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return namespaceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Ingress";
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
        NamespacedRef ingressRef = NamespacedRef.fromString(source.externalId());

        Optional<ExternalResource> namespace = namespaceHandler.describeExternalResource(
                account, ingressRef.namespace()
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
