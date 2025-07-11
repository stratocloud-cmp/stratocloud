package com.stratocloud.kubernetes.stateful.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.stateful.KubernetesStatefulSetHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.ResourceActionHandler;
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
import java.util.Set;

@Slf4j
@Component
public class KubernetesStatefulSetUpdateHandler implements ResourceActionHandler {

    private final KubernetesStatefulSetHandler statefulSetHandler;

    public KubernetesStatefulSetUpdateHandler(KubernetesStatefulSetHandler statefulSetHandler) {
        this.statefulSetHandler = statefulSetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return statefulSetHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新StatefulSet";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesStatefulSetUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1StatefulSet> statefulSet = statefulSetHandler.describeStatefulSet(
                account, resource.getExternalId()
        );

        if(statefulSet.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesStatefulSetUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(statefulSet.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updateStatefulSet(resource, parameters, false);
    }

    private void updateStatefulSet(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesStatefulSetUpdateInput input = JSON.convert(parameters, KubernetesStatefulSetUpdateInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when updating stateful set")
        );

        V1StatefulSet statefulSet = KubeUtil.fromYaml(input.getYamlContent(), V1StatefulSet.class);

        KubernetesProvider provider = (KubernetesProvider) statefulSetHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updateStatefulSet(
                namespace.getExternalId(), statefulSet, dryRun
        );
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
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        updateStatefulSet(resource, parameters, true);
    }
}
