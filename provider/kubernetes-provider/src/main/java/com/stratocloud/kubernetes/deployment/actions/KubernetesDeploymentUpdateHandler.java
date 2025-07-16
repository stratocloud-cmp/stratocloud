package com.stratocloud.kubernetes.deployment.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.deployment.KubernetesDeploymentHandler;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Deployment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class KubernetesDeploymentUpdateHandler implements ResourceActionHandler {

    private final KubernetesDeploymentHandler deploymentHandler;

    public KubernetesDeploymentUpdateHandler(KubernetesDeploymentHandler deploymentHandler) {
        this.deploymentHandler = deploymentHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return deploymentHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新Deployment";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesDeploymentUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1Deployment> deployment = deploymentHandler.describeDeployment(account, resource.getExternalId());

        if(deployment.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesDeploymentUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(deployment.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updateDeployment(resource, parameters, false);
    }

    private void updateDeployment(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesDeploymentUpdateInput input = JSON.convert(parameters, KubernetesDeploymentUpdateInput.class);

        Resource namespace = resource.getEssentialTargetByType(KubernetesNamespaceHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("Namespace not found when updating deployment")
        );

        V1Deployment deployment = KubeUtil.fromYaml(input.getYamlContent(), V1Deployment.class);

        KubernetesProvider provider = (KubernetesProvider) deploymentHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updateDeployment(
                namespace.getExternalId(), deployment, dryRun
        );
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var deployment = deploymentHandler.describeExternalResource(account, resource.getExternalId());

        if(deployment.isPresent() && deployment.get().state() == ResourceState.STARTING){
            log.warn("Deployment not started yet: {}", resource.getName());
            return ResourceActionResult.inProgress();
        }

        deploymentHandler.managePodsAndVolumes(resource);
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        updateDeployment(resource, parameters, true);
    }
}
