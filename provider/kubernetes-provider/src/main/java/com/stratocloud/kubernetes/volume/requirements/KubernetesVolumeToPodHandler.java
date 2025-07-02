package com.stratocloud.kubernetes.volume.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.kubernetes.pod.KubernetesPodHandler;
import com.stratocloud.kubernetes.volume.KubernetesPodVolumeHandler;
import com.stratocloud.kubernetes.volume.PodVolume;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesVolumeToPodHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "KUBERNETES_VOLUME_TO_POD_RELATIONSHIP";
    private final KubernetesPodVolumeHandler volumeHandler;

    private final KubernetesPodHandler podHandler;

    public KubernetesVolumeToPodHandler(KubernetesPodVolumeHandler volumeHandler,
                                        KubernetesPodHandler podHandler) {
        this.volumeHandler = volumeHandler;
        this.podHandler = podHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s Pod与Volume";
    }

    @Override
    public ResourceHandler getSource() {
        return volumeHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return podHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Volume";
    }

    @Override
    public String getRequirementName() {
        return "Pod";
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
    public boolean visibleInForm() {
        return false;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<PodVolume> podVolume = volumeHandler.describePodVolume(account, source.externalId());

        if(podVolume.isEmpty())
            return List.of();

        NamespacedRef podRef = podVolume.get().id().podRef();

        Optional<ExternalResource> pod = podHandler.describeExternalResource(account, podRef.toString());

        return pod.map(
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
