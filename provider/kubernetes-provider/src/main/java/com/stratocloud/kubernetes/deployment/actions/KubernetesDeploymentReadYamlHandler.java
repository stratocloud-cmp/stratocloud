package com.stratocloud.kubernetes.deployment.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.deployment.KubernetesDeploymentHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1Deployment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesDeploymentReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesDeploymentHandler deploymentHandler;

    public KubernetesDeploymentReadYamlHandler(KubernetesDeploymentHandler deploymentHandler) {
        this.deploymentHandler = deploymentHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return deploymentHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.READ_YAML;
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public List<ResourceReadActionResult> performReadAction(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1Deployment> deployment = deploymentHandler.describeDeployment(account, resource.getExternalId());

        if(deployment.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "Deployment Yaml",
                KubeUtil.toYaml(deployment.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
