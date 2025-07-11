package com.stratocloud.kubernetes.deployment.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.deployment.KubernetesDeploymentHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceState;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Deployment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
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

        resource.setExternalId(KubeUtil.getNamespacedRef(result.getMetadata()).toString());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var deployment = deploymentHandler.describeExternalResource(account, resource.getExternalId());

        if(deployment.isEmpty())
            return ResourceActionResult.failed("Deployment not found");

        ResourceState state = deployment.get().state();
        if(state == ResourceState.STARTING){
            log.warn("Deployment not started yet: {}", resource.getName());
            return ResourceActionResult.inProgress();
        }else if(state == ResourceState.ERROR){
            return ResourceActionResult.failed("Deployment is in error state");
        }else {
            deploymentHandler.managePodsAndVolumes(resource);
            return ResourceActionResult.finished();
        }
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
