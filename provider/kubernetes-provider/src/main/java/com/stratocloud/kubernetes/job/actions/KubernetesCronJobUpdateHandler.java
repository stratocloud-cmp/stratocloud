package com.stratocloud.kubernetes.job.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.job.KubernetesCronJobHandler;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1CronJob;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesCronJobUpdateHandler implements ResourceActionHandler {

    private final KubernetesCronJobHandler cronJobHandler;

    public KubernetesCronJobUpdateHandler(KubernetesCronJobHandler cronJobHandler) {
        this.cronJobHandler = cronJobHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cronJobHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新CronJob";
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
        return KubernetesCronJobUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1CronJob> cronJob = cronJobHandler.describeCronJob(account, resource.getExternalId());

        if(cronJob.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesCronJobUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(cronJob.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updateCronJob(resource, parameters, false);
    }

    private void updateCronJob(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesCronJobUpdateInput input = JSON.convert(parameters, KubernetesCronJobUpdateInput.class);

        Resource namespace = resource.getEssentialTargetByType(KubernetesNamespaceHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("Namespace not found when updating deployment")
        );

        V1CronJob cronJob = KubeUtil.fromYaml(input.getYamlContent(), V1CronJob.class);

        KubernetesProvider provider = (KubernetesProvider) cronJobHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updateCronJob(
                namespace.getExternalId(), cronJob, dryRun
        );
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        cronJobHandler.managePodsAndVolumes(resource);
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        updateCronJob(resource, parameters, true);
    }
}
