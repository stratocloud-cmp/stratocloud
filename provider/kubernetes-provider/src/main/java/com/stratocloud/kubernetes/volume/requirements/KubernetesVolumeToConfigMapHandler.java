package com.stratocloud.kubernetes.volume.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.config.KubernetesConfigMapHandler;
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
public class KubernetesVolumeToConfigMapHandler implements ExclusiveRequirementHandler {

    private final KubernetesPodVolumeHandler volumeHandler;

    private final KubernetesConfigMapHandler configMapHandler;

    public KubernetesVolumeToConfigMapHandler(KubernetesPodVolumeHandler volumeHandler,
                                              KubernetesConfigMapHandler configMapHandler) {
        this.volumeHandler = volumeHandler;
        this.configMapHandler = configMapHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_VOLUME_TO_CONFIG_MAP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Volume与ConfigMap";
    }

    @Override
    public ResourceHandler getSource() {
        return volumeHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return configMapHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Volume";
    }

    @Override
    public String getRequirementName() {
        return "ConfigMap";
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
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
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

        if(podVolume.get().volume().getConfigMap() == null)
            return List.of();

        NamespacedRef configMapRef = new NamespacedRef(
                podVolume.get().id().podRef().namespace(),
                podVolume.get().volume().getConfigMap().getName()
        );

        Optional<ExternalResource> configMap = configMapHandler.describeExternalResource(account, configMapRef.toString());

        return configMap.map(
                c -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                c,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
