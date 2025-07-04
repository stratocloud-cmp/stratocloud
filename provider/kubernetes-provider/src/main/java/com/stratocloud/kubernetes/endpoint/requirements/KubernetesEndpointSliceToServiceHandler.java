package com.stratocloud.kubernetes.endpoint.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.endpoint.KubernetesEndpointSliceHandler;
import com.stratocloud.kubernetes.service.KubernetesServiceHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import io.kubernetes.client.openapi.models.V1EndpointSlice;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesEndpointSliceToServiceHandler implements EssentialRequirementHandler {

    private final KubernetesEndpointSliceHandler endpointSliceHandler;

    private final KubernetesServiceHandler serviceHandler;

    public KubernetesEndpointSliceToServiceHandler(KubernetesEndpointSliceHandler endpointSliceHandler,
                                                   KubernetesServiceHandler serviceHandler) {
        this.endpointSliceHandler = endpointSliceHandler;
        this.serviceHandler = serviceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_ENDPOINT_SLICE_TO_SERVICE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Service与EndpointSlice";
    }

    @Override
    public ResourceHandler getSource() {
        return endpointSliceHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return serviceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "EndpointSlice";
    }

    @Override
    public String getRequirementName() {
        return "Service";
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
    public boolean visibleInForm() {
        return false;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<V1EndpointSlice> endpointSlice = endpointSliceHandler.describeEndpointSlice(
                account, source.externalId()
        );

        if(endpointSlice.isEmpty())
            return List.of();

        Optional<V1OwnerReference> ownerReference = KubeUtil.getOwnerReference(
                endpointSlice.get().getMetadata(), "Service"
        );

        if(ownerReference.isEmpty())
            return List.of();

        NamespacedRef endpointSliceRef = NamespacedRef.fromString(source.externalId());

        Optional<ExternalResource> service = serviceHandler.describeExternalResource(
                account,
                new NamespacedRef(endpointSliceRef.namespace(), ownerReference.get().getName()).toString()
        );

        return service.map(
                er -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                er,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
