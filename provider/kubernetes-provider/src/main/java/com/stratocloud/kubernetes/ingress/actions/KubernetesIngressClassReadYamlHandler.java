package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.ingress.KubernetesIngressClassHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1IngressClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesIngressClassReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesIngressClassHandler ingressClassHandler;

    public KubernetesIngressClassReadYamlHandler(KubernetesIngressClassHandler ingressClassHandler) {
        this.ingressClassHandler = ingressClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ingressClassHandler;
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
        Optional<V1IngressClass> ingressClass = ingressClassHandler.describeIngressClass(
                account, resource.getExternalId()
        );

        if(ingressClass.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "IngressClass Yaml",
                KubeUtil.toYaml(ingressClass.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
