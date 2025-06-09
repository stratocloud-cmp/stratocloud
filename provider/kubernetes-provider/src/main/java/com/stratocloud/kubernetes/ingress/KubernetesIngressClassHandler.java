package com.stratocloud.kubernetes.ingress;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.openapi.models.V1IngressClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesIngressClassHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesIngressClassHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_INGRESS_CLASS";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s IngressClass";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.INGRESS_CLASS;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeIngressClass(account, externalId).map(
                i -> toExternalResource(account, i)
        );
    }

    public Optional<V1IngressClass> describeIngressClass(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeIngressClass(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1IngressClass ingressClass) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getObjectName(ingressClass.getMetadata()),
                KubeUtil.getObjectName(ingressClass.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeIngressClasses().stream().map(
                i -> toExternalResource(account, i)
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
