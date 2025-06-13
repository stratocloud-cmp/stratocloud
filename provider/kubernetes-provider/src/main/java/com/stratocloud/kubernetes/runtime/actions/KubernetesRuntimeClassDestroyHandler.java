package com.stratocloud.kubernetes.runtime.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.runtime.KubernetesRuntimeClassHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1RuntimeClass;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesRuntimeClassDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesRuntimeClassHandler runtimeClassHandler;

    public KubernetesRuntimeClassDestroyHandler(KubernetesRuntimeClassHandler runtimeClassHandler) {
        this.runtimeClassHandler = runtimeClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return runtimeClassHandler;
    }

    @Override
    public String getTaskName() {
        return "删除RuntimeClass";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteRuntimeClass(resource, false);
    }

    private void deleteRuntimeClass(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) runtimeClassHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1RuntimeClass> runtimeClass = runtimeClassHandler.describeRuntimeClass(account, resource.getExternalId());

        if(runtimeClass.isEmpty())
            return;

        provider.buildClient(account).deleteRuntimeClass(resource.getExternalId(), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteRuntimeClass(resource, true);
    }
}
