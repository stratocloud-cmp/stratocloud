package com.stratocloud.kubernetes.node.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.node.KubernetesNodeHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Node;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesNodeBuildHandler implements BuildResourceActionHandler {

    private final KubernetesNodeHandler nodeHandler;

    public KubernetesNodeBuildHandler(KubernetesNodeHandler nodeHandler) {
        this.nodeHandler = nodeHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return nodeHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Node";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesNodeBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createNode(resource, parameters, false);
    }

    private void createNode(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) nodeHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesNodeBuildInput.class);

        V1Node node = KubeUtil.fromYaml(input.getYamlContent(), V1Node.class);
        V1Node result = provider.buildClient(account).createNode(node, dryRun);

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createNode(resource, parameters, true);
    }
}
