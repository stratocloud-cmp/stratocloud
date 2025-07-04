package com.stratocloud.kubernetes.stateful.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.stateful.KubernetesStatefulSetHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesStatefulSetReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesStatefulSetHandler statefulSetHandler;

    public KubernetesStatefulSetReadYamlHandler(KubernetesStatefulSetHandler statefulSetHandler) {
        this.statefulSetHandler = statefulSetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return statefulSetHandler;
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
        Optional<V1StatefulSet> statefulSet = statefulSetHandler.describeStatefulSet(account, resource.getExternalId());

        if(statefulSet.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "StatefulSet Yaml",
                KubeUtil.toYaml(statefulSet.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
