package com.stratocloud.kubernetes.job.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.job.KubernetesJobHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1Job;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesJobDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesJobHandler jobHandler;

    public KubernetesJobDestroyHandler(KubernetesJobHandler jobHandler) {
        this.jobHandler = jobHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return jobHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Job";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deletePod(resource, false);
    }

    private void deletePod(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) jobHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1Job> job = jobHandler.describeJob(account, resource.getExternalId());

        if(job.isEmpty())
            return;

        provider.buildClient(account).deleteJob(KubeUtil.getNamespacedRef(job.get().getMetadata()), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deletePod(resource, true);
    }
}
