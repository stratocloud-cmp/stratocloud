package com.stratocloud.kubernetes.node.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.node.KubernetesNodeHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1Node;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesNodeReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesNodeHandler nodeHandler;

    public KubernetesNodeReadYamlHandler(KubernetesNodeHandler nodeHandler) {
        this.nodeHandler = nodeHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return nodeHandler;
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
        Optional<V1Node> node = nodeHandler.describeNode(
                account, resource.getExternalId()
        );

        if(node.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "Node Yaml",
                KubeUtil.toYaml(node.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
