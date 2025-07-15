package com.stratocloud.kubernetes.job;

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
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesCronJobHandler extends AbstractResourceHandler implements EventAwareResourceHandler {

    private final KubernetesProvider provider;

    private final KubernetesManagementService managementService;

    public KubernetesCronJobHandler(KubernetesProvider provider,
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
        return "KUBERNETES_CRON_JOB";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s CronJob";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.CONTAINER_CRON_JOB;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeCronJob(account, externalId).map(
                c -> toExternalResource(account, c)
        );
    }

    public Optional<V1CronJob> describeCronJob(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeCronJob(NamespacedRef.fromString(externalId));
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1CronJob cronJob) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getNamespacedRef(cronJob.getMetadata()).toString(),
                KubeUtil.getObjectName(cronJob.getMetadata()),
                ResourceState.STARTED
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeCronJobs().stream().map(
                c -> toExternalResource(account, c)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource externalResource = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("CronJob not found")
        );
        resource.updateByExternal(externalResource);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    public void managePodsAndVolumes(Resource resource){
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1CronJob> cronJob = describeCronJob(
                account, resource.getExternalId()
        );

        if(cronJob.isEmpty())
            return;

        V1ObjectMeta metadata = cronJob.get().getMetadata();

        if(metadata == null)
            return;

        managementService.managePodsAndVolumes(
                provider,
                account,
                "CronJob",
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
                "CronJob",
                getResourceTypeId(),
                externalId,
                happenedAfter,
                true
        );
    }
}
