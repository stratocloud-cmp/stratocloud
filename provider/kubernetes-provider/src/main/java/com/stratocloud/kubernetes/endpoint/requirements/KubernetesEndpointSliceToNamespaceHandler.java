package com.stratocloud.kubernetes.endpoint.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.endpoint.KubernetesEndpointSliceHandler;
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
public class KubernetesEndpointSliceToNamespaceHandler implements EssentialRequirementHandler {

    private final KubernetesEndpointSliceHandler endpointSliceHandler;

    private final KubernetesNamespaceHandler namespaceHandler;

    public KubernetesEndpointSliceToNamespaceHandler(KubernetesEndpointSliceHandler endpointSliceHandler,
                                                     KubernetesNamespaceHandler namespaceHandler) {
        this.endpointSliceHandler = endpointSliceHandler;
        this.namespaceHandler = namespaceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_ENDPOINT_SLICE_TO_NAMESPACE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s EndpointSlice与Namespace";
    }

    @Override
    public ResourceHandler getSource() {
        return endpointSliceHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return namespaceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "EndpointSlice";
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
        NamespacedRef endpointSliceRef = NamespacedRef.fromString(source.externalId());

        Optional<ExternalResource> namespace = namespaceHandler.describeExternalResource(
                account, endpointSliceRef.namespace()
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
