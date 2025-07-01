package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.ingress.KubernetesIngressHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1Ingress;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesIngressDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesIngressHandler ingressHandler;

    public KubernetesIngressDestroyHandler(KubernetesIngressHandler ingressHandler) {
        this.ingressHandler = ingressHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ingressHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Ingress";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteIngress(resource, false);
    }

    private void deleteIngress(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) ingressHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1Ingress> ingress = ingressHandler.describeIngress(account, resource.getExternalId());

        if(ingress.isEmpty())
            return;

        provider.buildClient(account).deleteIngress(
                KubeUtil.getNamespacedRef(ingress.get().getMetadata()), dryRun
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteIngress(resource, true);
    }
}
