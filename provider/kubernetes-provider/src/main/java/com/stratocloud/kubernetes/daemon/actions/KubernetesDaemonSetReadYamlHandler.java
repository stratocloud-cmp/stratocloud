package com.stratocloud.kubernetes.daemon.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.daemon.KubernetesDaemonSetHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesDaemonSetReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesDaemonSetHandler daemonSetHandler;

    public KubernetesDaemonSetReadYamlHandler(KubernetesDaemonSetHandler daemonSetHandler) {
        this.daemonSetHandler = daemonSetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return daemonSetHandler;
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
        Optional<V1DaemonSet> daemonSet = daemonSetHandler.describeDaemonSet(account, resource.getExternalId());

        if(daemonSet.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "DaemonSet Yaml",
                KubeUtil.toYaml(daemonSet.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
