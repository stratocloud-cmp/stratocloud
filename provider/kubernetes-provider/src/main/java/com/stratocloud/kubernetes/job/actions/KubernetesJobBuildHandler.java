package com.stratocloud.kubernetes.job.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.job.KubernetesJobHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Job;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesJobBuildHandler implements BuildResourceActionHandler {

    private final KubernetesJobHandler jobHandler;

    public KubernetesJobBuildHandler(KubernetesJobHandler jobHandler) {
        this.jobHandler = jobHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return jobHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Job";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesJobBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createJob(resource, parameters, false);
    }

    private void createJob(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) jobHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesJobBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating job")
        );

        V1Job job = KubeUtil.fromYaml(input.getYamlContent(), V1Job.class);
        V1Job result = provider.buildClient(account).createJob(namespace.getExternalId(), job, dryRun);

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createJob(resource, parameters, true);
    }
}
