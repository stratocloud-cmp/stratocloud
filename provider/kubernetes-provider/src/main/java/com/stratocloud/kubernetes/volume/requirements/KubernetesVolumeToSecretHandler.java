package com.stratocloud.kubernetes.volume.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.config.KubernetesSecretHandler;
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
public class KubernetesVolumeToSecretHandler implements ExclusiveRequirementHandler {

    private final KubernetesPodVolumeHandler volumeHandler;

    private final KubernetesSecretHandler secretHandler;

    public KubernetesVolumeToSecretHandler(KubernetesPodVolumeHandler volumeHandler,
                                           KubernetesSecretHandler secretHandler) {
        this.volumeHandler = volumeHandler;
        this.secretHandler = secretHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_VOLUME_TO_SECRET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Volume与Secret";
    }

    @Override
    public ResourceHandler getSource() {
        return volumeHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return secretHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Volume";
    }

    @Override
    public String getRequirementName() {
        return "Secret";
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

        if(podVolume.get().volume().getSecret() == null)
            return List.of();

        NamespacedRef secretRef = new NamespacedRef(
                podVolume.get().id().podRef().namespace(),
                podVolume.get().volume().getSecret().getSecretName()
        );

        Optional<ExternalResource> secret = secretHandler.describeExternalResource(account, secretRef.toString());

        return secret.map(
                s -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                s,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
