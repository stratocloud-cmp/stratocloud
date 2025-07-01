package com.stratocloud.kubernetes.job.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.job.KubernetesCronJobHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1CronJob;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesCronJobDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesCronJobHandler cronJobHandler;

    public KubernetesCronJobDestroyHandler(KubernetesCronJobHandler cronJobHandler) {
        this.cronJobHandler = cronJobHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cronJobHandler;
    }

    @Override
    public String getTaskName() {
        return "删除CronJob";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteCronJob(resource, false);
    }

    private void deleteCronJob(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) cronJobHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1CronJob> job = cronJobHandler.describeCronJob(account, resource.getExternalId());

        if(job.isEmpty())
            return;

        provider.buildClient(account).deleteCronJob(KubeUtil.getNamespacedRef(job.get().getMetadata()), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteCronJob(resource, true);
    }
}
