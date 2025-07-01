package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.persistence.KubernetesStorageClassHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1StorageClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesStorageClassBuildHandler implements BuildResourceActionHandler {

    private final KubernetesStorageClassHandler storageClassHandler;

    public KubernetesStorageClassBuildHandler(KubernetesStorageClassHandler storageClassHandler) {
        this.storageClassHandler = storageClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return storageClassHandler;
    }

    @Override
    public String getTaskName() {
        return "创建StorageClass";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesStorageClassBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createStorageClass(resource, parameters, false);
    }

    private void createStorageClass(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) storageClassHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesStorageClassBuildInput.class);

        V1StorageClass storageClass = KubeUtil.fromYaml(input.getYamlContent(), V1StorageClass.class);
        V1StorageClass result = provider.buildClient(account).createStorageClass(storageClass, dryRun);

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createStorageClass(resource, parameters, true);
    }
}
