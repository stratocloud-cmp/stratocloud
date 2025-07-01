package com.stratocloud.kubernetes.deployment.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.deployment.KubernetesDeploymentHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1Deployment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesDeploymentDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesDeploymentHandler deploymentHandler;

    public KubernetesDeploymentDestroyHandler(KubernetesDeploymentHandler deploymentHandler) {
        this.deploymentHandler = deploymentHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return deploymentHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Deployment";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteDeployment(resource, false);
    }

    private void deleteDeployment(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) deploymentHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1Deployment> deployment = deploymentHandler.describeDeployment(
                account, resource.getExternalId()
        );

        if(deployment.isEmpty())
            return;

        provider.buildClient(account).deleteDeployment(
                KubeUtil.getNamespacedRef(deployment.get().getMetadata()), dryRun
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteDeployment(resource, true);
    }
}
