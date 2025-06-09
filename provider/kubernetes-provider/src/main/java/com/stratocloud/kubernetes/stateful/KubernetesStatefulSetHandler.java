package com.stratocloud.kubernetes.stateful;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesStatefulSetHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesStatefulSetHandler(KubernetesProvider provider) {
        this.provider = provider;
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
        return ResourceCategories.STATEFUL_SET;
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
                ResourceState.AVAILABLE
        );
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
        Optional<ExternalResource> externalResource = describeExternalResource(account, resource.getExternalId());
        externalResource.ifPresent(resource::updateByExternal);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
