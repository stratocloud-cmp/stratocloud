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
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.concurrent.SleepUtil;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
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

        resource.setExternalId(KubeUtil.getNamespacedRef(result.getMetadata()).toString());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ExternalResource> statefulSet = statefulSetHandler.describeExternalResource(
                account, resource.getExternalId()
        );

        if(statefulSet.isPresent() && statefulSet.get().state() == ResourceState.STARTING){
            log.warn("StatefulSet {} is not started yet.", statefulSet.get().name());
            SleepUtil.sleep(30);
        }
        statefulSetHandler.managePodsAndVolumes(resource);
        return ResourceActionResult.finished();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createStatefulSet(resource, parameters, true);
    }
}
