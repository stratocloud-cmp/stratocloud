package com.stratocloud.kubernetes.pod.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.pod.KubernetesPodHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesPodReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesPodHandler podHandler;

    public KubernetesPodReadYamlHandler(KubernetesPodHandler podHandler) {
        this.podHandler = podHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return podHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.READ_YAML;
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.values());
    }

    @Override
    public List<ResourceReadActionResult> performReadAction(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1Pod> pod = podHandler.describePod(account, resource.getExternalId());

        if(pod.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "Pod Yaml",
                KubeUtil.toYaml(pod.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
