package com.stratocloud.kubernetes.endpoint.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.endpoint.KubernetesEndpointSliceHandler;
import com.stratocloud.kubernetes.ingress.actions.KubernetesIngressBuildInput;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1EndpointSlice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesEndpointSliceBuildHandler implements BuildResourceActionHandler {

    private final KubernetesEndpointSliceHandler endpointSliceHandler;

    public KubernetesEndpointSliceBuildHandler(KubernetesEndpointSliceHandler endpointSliceHandler) {
        this.endpointSliceHandler = endpointSliceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return endpointSliceHandler;
    }

    @Override
    public String getTaskName() {
        return "创建EndpointSlice";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesEndpointSliceBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createEndpointSlice(resource, parameters, false);
    }

    private void createEndpointSlice(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) endpointSliceHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesIngressBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating endpoint slice")
        );

        V1EndpointSlice endpointSlice = KubeUtil.fromYaml(input.getYamlContent(), V1EndpointSlice.class);
        V1EndpointSlice result = provider.buildClient(account).createEndpointSlice(
                namespace.getExternalId(), endpointSlice, dryRun
        );

        resource.setExternalId(KubeUtil.getNamespacedRef(result.getMetadata()).toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createEndpointSlice(resource, parameters, true);
    }
}
