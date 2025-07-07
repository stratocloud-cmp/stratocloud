package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.ingress.KubernetesIngressHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1Ingress;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesIngressReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesIngressHandler ingressHandler;

    public KubernetesIngressReadYamlHandler(KubernetesIngressHandler ingressHandler) {
        this.ingressHandler = ingressHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ingressHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.READ_YAML;
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public List<ResourceReadActionResult> performReadAction(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1Ingress> ingress = ingressHandler.describeIngress(account, resource.getExternalId());

        if(ingress.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "Ingress Yaml",
                KubeUtil.toYaml(ingress.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
