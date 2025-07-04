package com.stratocloud.kubernetes.service.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.service.KubernetesServiceHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesServiceReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesServiceHandler serviceHandler;

    public KubernetesServiceReadYamlHandler(KubernetesServiceHandler serviceHandler) {
        this.serviceHandler = serviceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serviceHandler;
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
        Optional<V1Service> service = serviceHandler.describeService(account, resource.getExternalId());

        if(service.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "Service Yaml",
                KubeUtil.toYaml(service.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
