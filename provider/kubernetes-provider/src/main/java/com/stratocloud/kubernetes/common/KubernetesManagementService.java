package com.stratocloud.kubernetes.common;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.endpoint.KubernetesEndpointSliceHandler;
import com.stratocloud.kubernetes.pod.KubernetesPodHandler;
import com.stratocloud.kubernetes.volume.KubernetesPodVolumeHandler;
import com.stratocloud.kubernetes.volume.PodVolume;
import com.stratocloud.kubernetes.volume.PodVolumeId;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceManagementService;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1EndpointSlice;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Volume;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class KubernetesManagementService {

    private final ResourceManagementService resourceManagementService;

    public KubernetesManagementService(ResourceManagementService resourceManagementService) {
        this.resourceManagementService = resourceManagementService;
    }

    @Transactional
    public void managePodsAndVolumes(KubernetesProvider provider,
                                     ExternalAccount account,
                                     String ownerObjectKind,
                                     V1ObjectMeta ownerObjectMeta,
                                     Long ownerUserId){
        KubernetesClient client = provider.buildClient(account);

        String namespace = ownerObjectMeta.getNamespace();

        if(Utils.isBlank(namespace)){
            log.warn("Namespace not provided, cannot manage pods and volumes.");
            return;
        }

        List<V1Pod> pods = client.describePodsByNamespace(namespace).stream().filter(
                p -> p.getMetadata() != null && p.getMetadata().getOwnerReferences() != null
        ).filter(
                p -> p.getMetadata().getOwnerReferences().stream().anyMatch(
                        or -> Objects.equals(or.getKind(), ownerObjectKind) &&
                                Objects.equals(or.getName(), ownerObjectMeta.getName())
                )
        ).toList();

        KubernetesPodHandler podHandler = (KubernetesPodHandler) provider.getResourceHandlerByCategory(
                ResourceCategories.POD.id()
        ).orElseThrow(
                () -> new StratoException("Pod handler not found")
        );

        KubernetesPodVolumeHandler volumeHandler = (KubernetesPodVolumeHandler) provider.getResourceHandlerByCategory(
                ResourceCategories.POD_VOLUME.id()
        ).orElseThrow(
                () -> new StratoException("Pod volume handler not found")
        );

        if(Utils.isEmpty(pods))
            return;

        for (V1Pod pod : pods) {
            ExternalResource externalPod = podHandler.toExternalResource(account, pod);

            try {
                resourceManagementService.manageExternalResource(ownerUserId, externalPod);
            }catch (Exception e){
                log.warn("Failed to manage pod. Pod={}.", pod.getMetadata(), e);
                continue;
            }

            if(pod.getSpec() != null && pod.getSpec().getVolumes() != null){
                for (V1Volume volume : pod.getSpec().getVolumes()) {
                    PodVolume podVolume = new PodVolume(
                            new PodVolumeId(
                                    KubeUtil.getNamespacedRef(pod.getMetadata()),
                                    volume.getName()
                            ),
                            volume
                    );

                    ExternalResource externalVolume = volumeHandler.toExternalResource(account, podVolume);

                    try {
                        resourceManagementService.manageExternalResource(ownerUserId, externalVolume);
                    }catch (Exception e){
                        log.warn("Failed to manage volume. Volume={}.", volume.getName(), e);
                    }
                }
            }
        }
    }


    public void manageEndpointSlice(Resource serviceResource){
        if(Utils.isBlank(serviceResource.getExternalId()))
            return;

        NamespacedRef serviceRef = NamespacedRef.fromString(serviceResource.getExternalId());
        KubernetesProvider provider = (KubernetesProvider) serviceResource.getResourceHandler().getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(serviceResource.getAccountId());

        var endpointSlices = provider.buildClient(account).describeEndpointSlicesByNamespace(
                serviceRef.namespace()
        ).stream().filter(
                p -> p.getMetadata() != null && p.getMetadata().getOwnerReferences() != null
        ).filter(
                p -> p.getMetadata().getOwnerReferences().stream().anyMatch(
                        or -> Objects.equals(or.getKind(), "Service") &&
                                Objects.equals(or.getName(), serviceRef.name())
                )
        ).toList();

        var optional = provider.getResourceHandlerByCategory(ResourceCategories.ENDPOINT_SLICE.id());

        if(optional.isEmpty())
            return;

        KubernetesEndpointSliceHandler endpointSliceHandler = (KubernetesEndpointSliceHandler) optional.get();

        for (V1EndpointSlice endpointSlice : endpointSlices) {
            ExternalResource externalResource = endpointSliceHandler.toExternalResource(account, endpointSlice);

            try {
                resourceManagementService.manageExternalResource(
                        serviceResource.getOwnerId(), externalResource
                );
            }catch (Exception e){
                log.warn("Failed to manage endpoint slice. EndpointSlice={}.", externalResource.name(), e);
            }
        }
    }
}
