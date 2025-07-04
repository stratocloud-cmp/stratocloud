package com.stratocloud.kubernetes.runtime.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.runtime.KubernetesRuntimeClassHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1RuntimeClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesRuntimeClassReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesRuntimeClassHandler runtimeClassHandler;

    public KubernetesRuntimeClassReadYamlHandler(KubernetesRuntimeClassHandler runtimeClassHandler) {
        this.runtimeClassHandler = runtimeClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return runtimeClassHandler;
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
        Optional<V1RuntimeClass> runtimeClass = runtimeClassHandler.describeRuntimeClass(
                account, resource.getExternalId()
        );

        if(runtimeClass.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "RuntimeClass Yaml",
                KubeUtil.toYaml(runtimeClass.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
