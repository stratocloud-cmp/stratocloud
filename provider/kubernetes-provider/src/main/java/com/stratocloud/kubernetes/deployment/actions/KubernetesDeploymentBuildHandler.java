package com.stratocloud.kubernetes.deployment.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.TaskState;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.deployment.KubernetesDeploymentHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Deployment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesDeploymentBuildHandler implements BuildResourceActionHandler {

    private final KubernetesDeploymentHandler deploymentHandler;

    public KubernetesDeploymentBuildHandler(KubernetesDeploymentHandler deploymentHandler) {
        this.deploymentHandler = deploymentHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return deploymentHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Deployment";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesDeploymentBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createDeployment(resource, parameters, false);
    }

    private void createDeployment(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) deploymentHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesDeploymentBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating deployment")
        );

        V1Deployment deployment = KubeUtil.fromYaml(input.getYamlContent(), V1Deployment.class);
        V1Deployment result = provider.buildClient(account).createDeployment(
                namespace.getExternalId(), deployment, dryRun
        );

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = BuildResourceActionHandler.super.checkActionResult(
                resource, parameters
        );

        if(result.taskState() == TaskState.FINISHED || result.taskState() == TaskState.FAILED){
            deploymentHandler.managePodsAndVolumes(resource);
        }

        return result;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createDeployment(resource, parameters, true);
    }
}
