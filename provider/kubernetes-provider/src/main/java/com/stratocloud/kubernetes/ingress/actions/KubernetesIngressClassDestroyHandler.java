package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.ingress.KubernetesIngressClassHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1IngressClass;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesIngressClassDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesIngressClassHandler ingressClassHandler;

    public KubernetesIngressClassDestroyHandler(KubernetesIngressClassHandler ingressClassHandler) {
        this.ingressClassHandler = ingressClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ingressClassHandler;
    }

    @Override
    public String getTaskName() {
        return "删除IngressClass";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteIngressClass(resource, false);
    }

    private void deleteIngressClass(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) ingressClassHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1IngressClass> ingressClass = ingressClassHandler.describeIngressClass(
                account, resource.getExternalId()
        );

        if(ingressClass.isEmpty())
            return;

        provider.buildClient(account).deleteIngressClass(
                KubeUtil.getObjectName(ingressClass.get().getMetadata()),
                dryRun
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteIngressClass(resource, true);
    }
}
