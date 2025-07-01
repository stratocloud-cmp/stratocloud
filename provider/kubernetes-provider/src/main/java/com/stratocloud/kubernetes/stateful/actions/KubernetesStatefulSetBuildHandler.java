package com.stratocloud.kubernetes.stateful.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.stateful.KubernetesStatefulSetHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesStatefulSetBuildHandler implements BuildResourceActionHandler {

    private final KubernetesStatefulSetHandler statefulSetHandler;

    public KubernetesStatefulSetBuildHandler(KubernetesStatefulSetHandler statefulSetHandler) {
        this.statefulSetHandler = statefulSetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return statefulSetHandler;
    }

    @Override
    public String getTaskName() {
        return "创建StatefulSet";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesStatefulSetBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createStatefulSet(resource, parameters, false);
    }

    private void createStatefulSet(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) statefulSetHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesStatefulSetBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating stateful set")
        );

        V1StatefulSet statefulSet = KubeUtil.fromYaml(input.getYamlContent(), V1StatefulSet.class);
        V1StatefulSet result = provider.buildClient(account).createStatefulSet(
                namespace.getExternalId(), statefulSet, dryRun
        );

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createStatefulSet(resource, parameters, true);
    }
}
