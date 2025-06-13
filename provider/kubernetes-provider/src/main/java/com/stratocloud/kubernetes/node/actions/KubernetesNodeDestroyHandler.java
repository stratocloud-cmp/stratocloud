package com.stratocloud.kubernetes.node.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.node.KubernetesNodeHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1Node;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesNodeDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesNodeHandler nodeHandler;

    public KubernetesNodeDestroyHandler(KubernetesNodeHandler nodeHandler) {
        this.nodeHandler = nodeHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return nodeHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Node";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteNode(resource, false);
    }

    private void deleteNode(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) nodeHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1Node> node = nodeHandler.describeNode(account, resource.getExternalId());

        if(node.isEmpty())
            return;

        provider.buildClient(account).deleteNode(resource.getExternalId(), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteNode(resource, true);
    }
}
