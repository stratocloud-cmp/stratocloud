package com.stratocloud.kubernetes.network.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.network.KubernetesNetworkPolicyHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesNetworkPolicyBuildHandler implements BuildResourceActionHandler {

    private final KubernetesNetworkPolicyHandler networkPolicyHandler;

    public KubernetesNetworkPolicyBuildHandler(KubernetesNetworkPolicyHandler networkPolicyHandler) {
        this.networkPolicyHandler = networkPolicyHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return networkPolicyHandler;
    }

    @Override
    public String getTaskName() {
        return "创建NetworkPolicy";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesNetworkPolicyBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createNetworkPolicy(resource, parameters, false);
    }

    private void createNetworkPolicy(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) networkPolicyHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesNetworkPolicyBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating network policy")
        );

        V1NetworkPolicy networkPolicy = KubeUtil.fromYaml(input.getYamlContent(), V1NetworkPolicy.class);
        V1NetworkPolicy result = provider.buildClient(account).createNetworkPolicy(
                namespace.getExternalId(), networkPolicy, dryRun
        );

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createNetworkPolicy(resource, parameters, true);
    }
}
