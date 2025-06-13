package com.stratocloud.kubernetes.pod.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.pod.KubernetesPodHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesPodBuildHandler implements BuildResourceActionHandler {

    private final KubernetesPodHandler podHandler;

    public KubernetesPodBuildHandler(KubernetesPodHandler podHandler) {
        this.podHandler = podHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return podHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Pod";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesPodBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createPod(resource, parameters, false);
    }

    private void createPod(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) podHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesPodBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating pod")
        );

        V1Pod pod = KubeUtil.fromYaml(input.getYamlContent(), V1Pod.class);
        V1Pod result = provider.buildClient(account).createPod(namespace.getExternalId(), pod, dryRun);

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createPod(resource, parameters, true);
    }
}
