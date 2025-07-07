package com.stratocloud.kubernetes.job.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.job.KubernetesJobHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1Job;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesJobReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesJobHandler jobHandler;

    public KubernetesJobReadYamlHandler(KubernetesJobHandler jobHandler) {
        this.jobHandler = jobHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return jobHandler;
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
        Optional<V1Job> job = jobHandler.describeJob(account, resource.getExternalId());

        if(job.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "Job Yaml",
                KubeUtil.toYaml(job.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
