package com.stratocloud.kubernetes.namespace.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesNamespaceBuildHandler implements BuildResourceActionHandler {

    private final KubernetesNamespaceHandler namespaceHandler;

    public KubernetesNamespaceBuildHandler(KubernetesNamespaceHandler namespaceHandler) {
        this.namespaceHandler = namespaceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return namespaceHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Namespace";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createNamespace(resource, false);
    }

    private void createNamespace(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) namespaceHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        V1Namespace namespace = new V1Namespace();
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setName(resource.getName());
        namespace.setMetadata(objectMeta);
        namespace.setSpec(new V1NamespaceSpec());
        V1Namespace result = provider.buildClient(account).createNamespace(namespace, dryRun);
        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createNamespace(resource, true);
    }
}
