package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.persistence.KubernetesPvHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class KubernetesPvUpdateHandler implements ResourceActionHandler {

    private final KubernetesPvHandler pvHandler;

    public KubernetesPvUpdateHandler(KubernetesPvHandler pvHandler) {
        this.pvHandler = pvHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pvHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新PV";
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
        return KubernetesPvUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1PersistentVolume> pv = pvHandler.describePersistentVolume(account, resource.getExternalId());

        if(pv.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesPvUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(pv.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updatePv(resource, parameters, false);
    }

    private void updatePv(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesPvUpdateInput input = JSON.convert(parameters, KubernetesPvUpdateInput.class);

        V1PersistentVolume pv = KubeUtil.fromYaml(input.getYamlContent(), V1PersistentVolume.class);

        KubernetesProvider provider = (KubernetesProvider) pvHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updatePersistentVolume(
                pv, dryRun
        );
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        updatePv(resource, parameters, true);
    }
}
