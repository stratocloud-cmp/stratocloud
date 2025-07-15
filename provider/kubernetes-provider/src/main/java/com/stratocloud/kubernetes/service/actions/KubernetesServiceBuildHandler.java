package com.stratocloud.kubernetes.service.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.TaskState;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.KubernetesManagementService;
import com.stratocloud.kubernetes.service.KubernetesServiceHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesServiceBuildHandler implements BuildResourceActionHandler {

    private final KubernetesServiceHandler serviceHandler;

    private final KubernetesManagementService managementService;

    public KubernetesServiceBuildHandler(KubernetesServiceHandler serviceHandler,
                                         KubernetesManagementService managementService) {
        this.serviceHandler = serviceHandler;
        this.managementService = managementService;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serviceHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Service";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesServiceBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createService(resource, parameters, false);
    }

    private void createService(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) serviceHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesServiceBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating service")
        );

        V1Service service = KubeUtil.fromYaml(input.getYamlContent(), V1Service.class);
        V1Service result = provider.buildClient(account).createService(namespace.getExternalId(), service, dryRun);

        resource.setExternalId(KubeUtil.getNamespacedRef(result.getMetadata()).toString());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = BuildResourceActionHandler.super.checkActionResult(resource, parameters);
        if(result.taskState() == TaskState.FINISHED || result.taskState() == TaskState.FAILED)
            managementService.manageEndpointSlice(resource);
        return result;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createService(resource, parameters, true);
    }
}
