package com.stratocloud.kubernetes.network.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.kubernetes.network.KubernetesNetworkPolicyHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class KubernetesNetworkPolicyUpdateHandler implements ResourceActionHandler {

    private final KubernetesNetworkPolicyHandler networkPolicyHandler;

    public KubernetesNetworkPolicyUpdateHandler(KubernetesNetworkPolicyHandler networkPolicyHandler) {
        this.networkPolicyHandler = networkPolicyHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return networkPolicyHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新NetworkPolicy";
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
        return KubernetesNetworkPolicyUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1NetworkPolicy> networkPolicy = networkPolicyHandler.describeNetworkPolicy(account, resource.getExternalId());

        if(networkPolicy.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesNetworkPolicyUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(networkPolicy.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updateNetworkPolicy(resource, parameters, false);
    }

    private void updateNetworkPolicy(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesNetworkPolicyUpdateInput input = JSON.convert(parameters, KubernetesNetworkPolicyUpdateInput.class);

        Resource namespace = resource.getEssentialTargetByType(KubernetesNamespaceHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("Namespace not found when updating network policy")
        );

        V1NetworkPolicy networkPolicy = KubeUtil.fromYaml(input.getYamlContent(), V1NetworkPolicy.class);

        KubernetesProvider provider = (KubernetesProvider) networkPolicyHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updateNetworkPolicy(
                namespace.getExternalId(), networkPolicy, dryRun
        );
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        updateNetworkPolicy(resource, parameters, true);
    }
}
