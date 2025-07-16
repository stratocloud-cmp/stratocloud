package com.stratocloud.kubernetes.endpoint;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1EndpointSlice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesEndpointSliceHandler extends AbstractResourceHandler {

    public static final String TYPE_ID = "KUBERNETES_ENDPOINT_SLICE";
    private final KubernetesProvider provider;

    public KubernetesEndpointSliceHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getResourceTypeName() {
        return "K8s EndpointSlice";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.CONTAINER_NETWORK;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeEndpointSlice(account, externalId).map(
                e -> toExternalResource(account, e)
        );
    }

    public Optional<V1EndpointSlice> describeEndpointSlice(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeEndpointSlice(NamespacedRef.fromString(externalId));
    }

    public ExternalResource toExternalResource(ExternalAccount account, V1EndpointSlice endpointSlice) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getNamespacedRef(endpointSlice.getMetadata()).toString(),
                KubeUtil.getObjectName(endpointSlice.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeEndpointSlices().stream().map(
                e -> toExternalResource(account, e)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource externalResource = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("EndpointSlice not found")
        );
        resource.updateByExternal(externalResource);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
