package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.ingress.KubernetesIngressClassHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1IngressClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesIngressClassBuildHandler implements BuildResourceActionHandler {

    private final KubernetesIngressClassHandler ingressClassHandler;

    public KubernetesIngressClassBuildHandler(KubernetesIngressClassHandler ingressClassHandler) {
        this.ingressClassHandler = ingressClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ingressClassHandler;
    }

    @Override
    public String getTaskName() {
        return "创建IngressClass";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesIngressBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createIngressClass(resource, parameters, false);
    }

    private void createIngressClass(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) ingressClassHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesIngressBuildInput.class);

        V1IngressClass ingressClass = KubeUtil.fromYaml(input.getYamlContent(), V1IngressClass.class);
        V1IngressClass result = provider.buildClient(account).createIngressClass(
                ingressClass, dryRun
        );

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createIngressClass(resource, parameters, true);
    }
}
