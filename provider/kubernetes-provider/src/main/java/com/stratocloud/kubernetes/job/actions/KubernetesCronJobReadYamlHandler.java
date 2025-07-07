package com.stratocloud.kubernetes.job.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.job.KubernetesCronJobHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1CronJob;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesCronJobReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesCronJobHandler cronJobHandler;

    public KubernetesCronJobReadYamlHandler(KubernetesCronJobHandler cronJobHandler) {
        this.cronJobHandler = cronJobHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cronJobHandler;
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
        Optional<V1CronJob> cronJob = cronJobHandler.describeCronJob(account, resource.getExternalId());

        if(cronJob.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "CronJob Yaml",
                KubeUtil.toYaml(cronJob.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
