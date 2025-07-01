package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.persistence.KubernetesStorageClassHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1StorageClass;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesStorageClassDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesStorageClassHandler storageClassHandler;

    public KubernetesStorageClassDestroyHandler(KubernetesStorageClassHandler storageClassHandler) {
        this.storageClassHandler = storageClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return storageClassHandler;
    }

    @Override
    public String getTaskName() {
        return "删除StorageClass";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteStorageClass(resource, false);
    }

    private void deleteStorageClass(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) storageClassHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1StorageClass> storageClass = storageClassHandler.describeStorageClass(account, resource.getExternalId());

        if(storageClass.isEmpty())
            return;

        provider.buildClient(account).deleteStorageClass(resource.getExternalId(), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteStorageClass(resource, true);
    }
}
