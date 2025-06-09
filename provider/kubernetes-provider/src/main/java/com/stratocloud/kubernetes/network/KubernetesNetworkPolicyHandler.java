package com.stratocloud.kubernetes.network;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesNetworkPolicyHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesNetworkPolicyHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_NETWORK_POLICY";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s NetworkPolicy";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.NETWORK_POLICY;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeNetworkPolicy(account, externalId).map(
                n -> toExternalResource(account, n)
        );
    }

    public Optional<V1NetworkPolicy> describeNetworkPolicy(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeNetworkPolicy(NamespacedRef.fromString(externalId));
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1NetworkPolicy networkPolicy) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getNamespacedRef(networkPolicy.getMetadata()).toString(),
                KubeUtil.getObjectName(networkPolicy.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeNetworkPolicies().stream().map(
                n -> toExternalResource(account, n)
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
