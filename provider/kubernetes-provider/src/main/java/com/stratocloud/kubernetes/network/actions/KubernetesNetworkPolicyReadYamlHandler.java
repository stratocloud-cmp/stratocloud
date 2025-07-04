package com.stratocloud.kubernetes.network.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.network.KubernetesNetworkPolicyHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesNetworkPolicyReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesNetworkPolicyHandler networkPolicyHandler;

    public KubernetesNetworkPolicyReadYamlHandler(KubernetesNetworkPolicyHandler networkPolicyHandler) {
        this.networkPolicyHandler = networkPolicyHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return networkPolicyHandler;
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
        Optional<V1NetworkPolicy> networkPolicy = networkPolicyHandler.describeNetworkPolicy(
                account, resource.getExternalId()
        );

        if(networkPolicy.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "NetworkPolicy Yaml",
                KubeUtil.toYaml(networkPolicy.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
