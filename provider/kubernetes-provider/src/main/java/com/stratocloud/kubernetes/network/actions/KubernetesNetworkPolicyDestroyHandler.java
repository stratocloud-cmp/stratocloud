package com.stratocloud.kubernetes.network.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.network.KubernetesNetworkPolicyHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesNetworkPolicyDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesNetworkPolicyHandler networkPolicyHandler;

    public KubernetesNetworkPolicyDestroyHandler(KubernetesNetworkPolicyHandler networkPolicyHandler) {
        this.networkPolicyHandler = networkPolicyHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return networkPolicyHandler;
    }

    @Override
    public String getTaskName() {
        return "删除NetworkPolicy";
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
        KubernetesProvider provider = (KubernetesProvider) networkPolicyHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1NetworkPolicy> networkPolicy = networkPolicyHandler.describeNetworkPolicy(
                account, resource.getExternalId()
        );

        if(networkPolicy.isEmpty())
            return;

        provider.buildClient(account).deleteNetworkPolicy(
                KubeUtil.getNamespacedRef(networkPolicy.get().getMetadata()),
                dryRun
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteNamespace(resource, true);
    }
}
