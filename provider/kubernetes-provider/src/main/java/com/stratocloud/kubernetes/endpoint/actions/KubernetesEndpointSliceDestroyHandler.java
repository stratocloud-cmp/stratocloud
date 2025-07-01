package com.stratocloud.kubernetes.endpoint.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.endpoint.KubernetesEndpointSliceHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1EndpointSlice;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesEndpointSliceDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesEndpointSliceHandler endpointSliceHandler;

    public KubernetesEndpointSliceDestroyHandler(KubernetesEndpointSliceHandler endpointSliceHandler) {
        this.endpointSliceHandler = endpointSliceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return endpointSliceHandler;
    }

    @Override
    public String getTaskName() {
        return "删除EndpointSlice";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteEndpointSlice(resource, false);
    }

    private void deleteEndpointSlice(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) endpointSliceHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1EndpointSlice> endpointSlice = endpointSliceHandler.describeEndpointSlice(
                account, resource.getExternalId()
        );

        if(endpointSlice.isEmpty())
            return;

        provider.buildClient(account).deleteEndpointSlice(
                KubeUtil.getNamespacedRef(endpointSlice.get().getMetadata()), dryRun
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteEndpointSlice(resource, true);
    }
}
