package com.stratocloud.kubernetes.pod.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.node.KubernetesNodeHandler;
import com.stratocloud.kubernetes.pod.KubernetesPodHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPodToNodeHandler implements EssentialRequirementHandler {

    private final KubernetesPodHandler podHandler;

    private final KubernetesNodeHandler nodeHandler;

    public KubernetesPodToNodeHandler(KubernetesPodHandler podHandler,
                                      KubernetesNodeHandler nodeHandler) {
        this.podHandler = podHandler;
        this.nodeHandler = nodeHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_POD_TO_NODE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Node与Pod";
    }

    @Override
    public ResourceHandler getSource() {
        return podHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return nodeHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Pod";
    }

    @Override
    public String getRequirementName() {
        return "Node";
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<V1Pod> pod = podHandler.describePod(account, source.externalId());

        if(pod.isEmpty())
            return List.of();

        V1PodSpec spec = pod.get().getSpec();

        if(spec == null || Utils.isBlank(spec.getNodeName()))
            return List.of();

        Optional<ExternalResource> node = nodeHandler.describeExternalResource(account, spec.getNodeName());

        if(node.isEmpty())
            return List.of();

        return node.map(
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
