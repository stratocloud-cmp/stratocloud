package com.stratocloud.kubernetes.namespace.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1Namespace;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesNamespaceDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesNamespaceHandler namespaceHandler;

    public KubernetesNamespaceDestroyHandler(KubernetesNamespaceHandler namespaceHandler) {
        this.namespaceHandler = namespaceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return namespaceHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Namespace";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteNamespace(resource, false);
    }

    private void deleteNamespace(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) namespaceHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1Namespace> namespace = namespaceHandler.describeNamespace(account, resource.getExternalId());

        if(namespace.isEmpty())
            return;

        provider.buildClient(account).deleteNamespace(resource.getExternalId(), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteNamespace(resource, true);
    }
}
