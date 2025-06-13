package com.stratocloud.kubernetes.runtime.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.runtime.KubernetesRuntimeClassHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1RuntimeClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesRuntimeClassBuildHandler implements BuildResourceActionHandler {

    private final KubernetesRuntimeClassHandler runtimeClassHandler;

    public KubernetesRuntimeClassBuildHandler(KubernetesRuntimeClassHandler runtimeClassHandler) {
        this.runtimeClassHandler = runtimeClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return runtimeClassHandler;
    }

    @Override
    public String getTaskName() {
        return "创建RuntimeClass";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesRuntimeClassBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createRuntimeClass(resource, parameters, false);
    }

    private void createRuntimeClass(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) runtimeClassHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesRuntimeClassBuildInput.class);

        V1RuntimeClass runtimeClass = KubeUtil.fromYaml(input.getYamlContent(), V1RuntimeClass.class);
        V1RuntimeClass result = provider.buildClient(account).createRuntimeClass(runtimeClass, dryRun);

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createRuntimeClass(resource, parameters, true);
    }
}
