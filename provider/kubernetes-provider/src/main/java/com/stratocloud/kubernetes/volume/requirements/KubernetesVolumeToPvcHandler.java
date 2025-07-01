package com.stratocloud.kubernetes.volume.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.persistence.KubernetesPvcHandler;
import com.stratocloud.kubernetes.volume.KubernetesPodVolumeHandler;
import com.stratocloud.kubernetes.volume.PodVolume;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesVolumeToPvcHandler implements ExclusiveRequirementHandler {

    private final KubernetesPodVolumeHandler volumeHandler;

    private final KubernetesPvcHandler pvcHandler;

    public KubernetesVolumeToPvcHandler(KubernetesPodVolumeHandler volumeHandler,
                                        KubernetesPvcHandler pvcHandler) {
        this.volumeHandler = volumeHandler;
        this.pvcHandler = pvcHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_VOLUME_TO_PVC_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Volume与PVC";
    }

    @Override
    public ResourceHandler getSource() {
        return volumeHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return pvcHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Volume";
    }

    @Override
    public String getRequirementName() {
        return "PVC";
    }

    @Override
    public String getConnectActionName() {
        return "挂载";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除挂载";
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
        Optional<PodVolume> podVolume = volumeHandler.describePodVolume(account, source.externalId());

        if(podVolume.isEmpty())
            return List.of();

        if(podVolume.get().volume().getPersistentVolumeClaim() == null)
            return List.of();

        NamespacedRef pvcRef = new NamespacedRef(
                podVolume.get().id().podRef().namespace(),
                podVolume.get().volume().getPersistentVolumeClaim().getClaimName()
        );

        Optional<ExternalResource> pvc = pvcHandler.describeExternalResource(account, pvcRef.toString());

        return pvc.map(
                p -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                p,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
