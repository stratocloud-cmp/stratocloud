package com.stratocloud.kubernetes.pod.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.daemon.KubernetesDaemonSetHandler;
import com.stratocloud.kubernetes.pod.KubernetesPodHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPodToDaemonSetHandler implements ExclusiveRequirementHandler {

    private final KubernetesPodHandler podHandler;

    private final KubernetesDaemonSetHandler daemonSetHandler;

    public KubernetesPodToDaemonSetHandler(KubernetesPodHandler podHandler,
                                           KubernetesDaemonSetHandler daemonSetHandler) {
        this.podHandler = podHandler;
        this.daemonSetHandler = daemonSetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_POD_TO_DAEMON_SET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s DaemonSet与Pod";
    }

    @Override
    public ResourceHandler getSource() {
        return podHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return daemonSetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Pod";
    }

    @Override
    public String getRequirementName() {
        return "DaemonSet";
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
    public boolean visibleInForm() {
        return false;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<V1Pod> pod = podHandler.describePod(account, source.externalId());

        if(pod.isEmpty())
            return List.of();

        Optional<V1OwnerReference> ownerReference = KubeUtil.getOwnerReference(
                pod.get().getMetadata(), "DaemonSet"
        );

        if(ownerReference.isEmpty())
            return List.of();

        NamespacedRef podRef = NamespacedRef.fromString(source.externalId());

        Optional<ExternalResource> daemonSet = daemonSetHandler.describeExternalResource(
                account,
                new NamespacedRef(podRef.namespace(), ownerReference.get().getName()).toString()
        );

        return daemonSet.map(er -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        er,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
