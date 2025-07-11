package com.stratocloud.kubernetes.daemon.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.daemon.KubernetesDaemonSetHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.concurrent.SleepUtil;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class KubernetesDaemonSetUpdateHandler implements ResourceActionHandler {

    private final KubernetesDaemonSetHandler daemonSetHandler;

    public KubernetesDaemonSetUpdateHandler(KubernetesDaemonSetHandler daemonSetHandler) {
        this.daemonSetHandler = daemonSetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return daemonSetHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新DaemonSet";
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
        return KubernetesDaemonSetUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1DaemonSet> daemonSet = daemonSetHandler.describeDaemonSet(account, resource.getExternalId());

        if(daemonSet.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesDaemonSetUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(daemonSet.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updateDaemonSet(resource, parameters, false);
    }

    private void updateDaemonSet(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesDaemonSetUpdateInput input = JSON.convert(parameters, KubernetesDaemonSetUpdateInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when updating daemon set")
        );

        V1DaemonSet daemonSet = KubeUtil.fromYaml(input.getYamlContent(), V1DaemonSet.class);

        KubernetesProvider provider = (KubernetesProvider) daemonSetHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updateDaemonSet(namespace.getExternalId(), daemonSet, dryRun);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ExternalResource> daemonSet = daemonSetHandler.describeExternalResource(
                account, resource.getExternalId()
        );

        if(daemonSet.isPresent() && daemonSet.get().state() == ResourceState.STARTING){
            log.warn("DaemonSet {} is not totally started yet.", daemonSet.get().name());
            SleepUtil.sleep(30);
        }

        daemonSetHandler.managePodsAndVolumes(resource);
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        updateDaemonSet(resource, parameters, true);
    }
}
