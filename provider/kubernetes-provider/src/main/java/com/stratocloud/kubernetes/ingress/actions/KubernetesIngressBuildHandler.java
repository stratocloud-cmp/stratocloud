package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.ingress.KubernetesIngressHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Ingress;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesIngressBuildHandler implements BuildResourceActionHandler {

    private final KubernetesIngressHandler ingressHandler;

    public KubernetesIngressBuildHandler(KubernetesIngressHandler ingressHandler) {
        this.ingressHandler = ingressHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ingressHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Ingress";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesIngressBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createIngress(resource, parameters, false);
    }

    private void createIngress(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) ingressHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesIngressBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating ingress")
        );

        V1Ingress ingress = KubeUtil.fromYaml(input.getYamlContent(), V1Ingress.class);
        V1Ingress result = provider.buildClient(account).createIngress(
                namespace.getExternalId(), ingress, dryRun
        );

        resource.setExternalId(KubeUtil.getNamespacedRef(result.getMetadata()).toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createIngress(resource, parameters, true);
    }
}
