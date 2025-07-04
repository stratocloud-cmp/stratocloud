package com.stratocloud.kubernetes.network.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.kubernetes.network.KubernetesNetworkPolicyHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesNetworkPolicyToNamespaceHandler implements EssentialRequirementHandler {

    private final KubernetesNetworkPolicyHandler networkPolicyHandler;

    private final KubernetesNamespaceHandler namespaceHandler;

    public KubernetesNetworkPolicyToNamespaceHandler(KubernetesNetworkPolicyHandler networkPolicyHandler,
                                                     KubernetesNamespaceHandler namespaceHandler) {
        this.networkPolicyHandler = networkPolicyHandler;
        this.namespaceHandler = namespaceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_NETWORK_POLICY_TO_NAMESPACE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s NetworkPolicy与Namespace";
    }

    @Override
    public ResourceHandler getSource() {
        return networkPolicyHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return namespaceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "NetworkPolicy";
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
        NamespacedRef networkPolicyRef = NamespacedRef.fromString(source.externalId());

        Optional<ExternalResource> namespace = namespaceHandler.describeExternalResource(
                account, networkPolicyRef.namespace()
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
