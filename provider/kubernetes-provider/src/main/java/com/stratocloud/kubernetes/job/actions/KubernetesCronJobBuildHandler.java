package com.stratocloud.kubernetes.job.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.TaskState;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.job.KubernetesCronJobHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1CronJob;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesCronJobBuildHandler implements BuildResourceActionHandler {

    private final KubernetesCronJobHandler cronJobHandler;

    public KubernetesCronJobBuildHandler(KubernetesCronJobHandler cronJobHandler) {
        this.cronJobHandler = cronJobHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cronJobHandler;
    }

    @Override
    public String getTaskName() {
        return "创建CronJob";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesCronJobBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createCronJob(resource, parameters, false);
    }

    private void createCronJob(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) cronJobHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesCronJobBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating cron job")
        );

        V1CronJob cronJob = KubeUtil.fromYaml(input.getYamlContent(), V1CronJob.class);
        V1CronJob result = provider.buildClient(account).createCronJob(namespace.getExternalId(), cronJob, dryRun);

        resource.setExternalId(KubeUtil.getNamespacedRef(result.getMetadata()).toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createCronJob(resource, parameters, true);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = BuildResourceActionHandler.super.checkActionResult(
                resource, parameters
        );

        if(result.taskState() == TaskState.FINISHED || result.taskState() == TaskState.FAILED)
            cronJobHandler.managePodsAndVolumes(resource);

        return result;
    }
}
