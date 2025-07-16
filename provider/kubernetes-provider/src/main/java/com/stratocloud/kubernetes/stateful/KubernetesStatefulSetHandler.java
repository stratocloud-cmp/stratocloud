package com.stratocloud.kubernetes.stateful;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.KubernetesManagementService;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.event.EventAwareResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesStatefulSetHandler extends AbstractResourceHandler implements EventAwareResourceHandler {

    private final KubernetesProvider provider;

    private final KubernetesManagementService managementService;

    public KubernetesStatefulSetHandler(KubernetesProvider provider,
                                        KubernetesManagementService managementService) {
        this.provider = provider;
        this.managementService = managementService;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_STATEFUL_SET";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s StatefulSet";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.CONTAINER_WORKLOAD;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeStatefulSet(account, externalId).map(
                s -> toExternalResource(account, s)
        );
    }

    public Optional<V1StatefulSet> describeStatefulSet(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeStatefulSet(NamespacedRef.fromString(externalId));
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1StatefulSet statefulSet) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getNamespacedRef(statefulSet.getMetadata()).toString(),
                KubeUtil.getObjectName(statefulSet.getMetadata()),
                convertState(statefulSet)
        );
    }

    private ResourceState convertState(V1StatefulSet statefulSet) {
        V1StatefulSetStatus status = statefulSet.getStatus();

        if(status == null)
            return ResourceState.UNKNOWN;

        int replicas = status.getReplicas();
        int readyReplicas = status.getReadyReplicas() != null ? status.getReadyReplicas() : 0;

        if(replicas == 0)
            return ResourceState.STOPPED;

        return replicas > readyReplicas ? ResourceState.STARTING : ResourceState.STARTED;
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeStatefulSets().stream().map(
                s -> toExternalResource(account, s)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource externalResource = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("StatefulSet not found")
        );
        resource.updateByExternal(externalResource);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    public void managePodsAndVolumes(Resource resource){
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1StatefulSet> statefulSet = describeStatefulSet(
                account, resource.getExternalId()
        );

        if(statefulSet.isEmpty())
            return;

        V1ObjectMeta metadata = statefulSet.get().getMetadata();

        if(metadata == null)
            return;

        managementService.managePodsAndVolumes(
                provider,
                account,
                "StatefulSet",
                metadata,
                resource.getOwnerId()
        );
    }

    @Override
    public List<ExternalResourceEvent> describeResourceEvents(ExternalAccount account,
                                                              String externalId,
                                                              LocalDateTime happenedAfter) {
        return KubeUtil.describeResourceEvents(
                provider,
                account,
                "StatefulSet",
                getResourceTypeId(),
                externalId,
                happenedAfter,
                true
        );
    }
}
