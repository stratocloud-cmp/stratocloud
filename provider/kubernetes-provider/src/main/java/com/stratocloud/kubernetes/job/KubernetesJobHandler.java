package com.stratocloud.kubernetes.job;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1Job;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesJobHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesJobHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_JOB";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s Job";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.CONTAINER_JOB;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeJob(account, externalId).map(
                j -> toExternalResource(account, j)
        );
    }

    public Optional<V1Job> describeJob(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeJob(NamespacedRef.fromString(externalId));
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1Job job) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getNamespacedRef(job.getMetadata()).toString(),
                KubeUtil.getObjectName(job.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeJobs().stream().map(
                j -> toExternalResource(account, j)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> externalResource = describeExternalResource(account, resource.getExternalId());
        externalResource.ifPresent(resource::updateByExternal);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
