package com.stratocloud.kubernetes.config.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.config.KubernetesConfigMapHandler;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesConfigMapBuildHandler implements BuildResourceActionHandler {

    private final KubernetesConfigMapHandler configMapHandler;

    public KubernetesConfigMapBuildHandler(KubernetesConfigMapHandler configMapHandler) {
        this.configMapHandler = configMapHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return configMapHandler;
    }

    @Override
    public String getTaskName() {
        return "创建ConfigMap";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesConfigMapBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createConfigMap(resource, parameters, false);
    }

    private void createConfigMap(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) configMapHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesConfigMapBuildInput.class);

        Resource namespace = resource.getEssentialTargetByType(KubernetesNamespaceHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("Namespace not found when creating config map")
        );

        V1ConfigMap configMap = KubeUtil.fromYaml(input.getYamlContent(), V1ConfigMap.class);
        V1ConfigMap result = provider.buildClient(account).createConfigMap(
                namespace.getExternalId(), configMap, dryRun
        );

        resource.setExternalId(KubeUtil.getNamespacedRef(result.getMetadata()).toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createConfigMap(resource, parameters, true);
    }
}
