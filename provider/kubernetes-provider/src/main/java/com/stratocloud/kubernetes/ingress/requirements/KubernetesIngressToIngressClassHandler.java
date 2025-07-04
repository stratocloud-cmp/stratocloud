package com.stratocloud.kubernetes.ingress.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.ingress.KubernetesIngressClassHandler;
import com.stratocloud.kubernetes.ingress.KubernetesIngressHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1IngressSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesIngressToIngressClassHandler implements EssentialRequirementHandler {

    private final KubernetesIngressHandler ingressHandler;

    private final KubernetesIngressClassHandler ingressClassHandler;

    public KubernetesIngressToIngressClassHandler(KubernetesIngressHandler ingressHandler,
                                                  KubernetesIngressClassHandler ingressClassHandler) {
        this.ingressHandler = ingressHandler;
        this.ingressClassHandler = ingressClassHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_INGRESS_TO_INGRESS_CLASS_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Ingress与IngressClass";
    }

    @Override
    public ResourceHandler getSource() {
        return ingressHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return ingressClassHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Ingress";
    }

    @Override
    public String getRequirementName() {
        return "IngressClass";
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
        Optional<V1Ingress> ingress = ingressHandler.describeIngress(
                account, source.externalId()
        );

        if(ingress.isEmpty())
            return List.of();

        V1IngressSpec spec = ingress.get().getSpec();

        if(spec == null)
            return List.of();

        String ingressClassName = spec.getIngressClassName();

        Optional<ExternalResource> ingressClass = ingressClassHandler.describeExternalResource(
                account, ingressClassName
        );

        return ingressClass.map(
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
