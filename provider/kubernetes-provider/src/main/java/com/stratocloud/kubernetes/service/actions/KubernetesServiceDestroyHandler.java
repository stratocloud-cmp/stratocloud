package com.stratocloud.kubernetes.service.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.service.KubernetesServiceHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesServiceDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesServiceHandler serviceHandler;

    public KubernetesServiceDestroyHandler(KubernetesServiceHandler serviceHandler) {
        this.serviceHandler = serviceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serviceHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Service";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deletePod(resource, false);
    }

    private void deletePod(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) serviceHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1Service> service = serviceHandler.describeService(account, resource.getExternalId());

        if(service.isEmpty())
            return;

        provider.buildClient(account).deleteService(KubeUtil.getNamespacedRef(service.get().getMetadata()), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deletePod(resource, true);
    }
}
