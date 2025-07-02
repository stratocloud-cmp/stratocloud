package com.stratocloud.kubernetes.daemon.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.TaskState;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.daemon.KubernetesDaemonSetHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesDaemonSetBuildHandler implements BuildResourceActionHandler {

    private final KubernetesDaemonSetHandler daemonSetHandler;

    public KubernetesDaemonSetBuildHandler(KubernetesDaemonSetHandler daemonSetHandler) {
        this.daemonSetHandler = daemonSetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return daemonSetHandler;
    }

    @Override
    public String getTaskName() {
        return "创建DaemonSet";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesDaemonSetBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createDaemonSet(resource, parameters, false);
    }

    private void createDaemonSet(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) daemonSetHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesDaemonSetBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating daemon set")
        );

        V1DaemonSet daemonSet = KubeUtil.fromYaml(input.getYamlContent(), V1DaemonSet.class);
        V1DaemonSet result = provider.buildClient(account).createDaemonSet(
                namespace.getExternalId(), daemonSet, dryRun
        );

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = BuildResourceActionHandler.super.checkActionResult(resource, parameters);

        if(result.taskState() == TaskState.FINISHED || result.taskState() == TaskState.FAILED)
            daemonSetHandler.managePodsAndVolumes(resource);

        return result;
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createDaemonSet(resource, parameters, true);
    }
}
