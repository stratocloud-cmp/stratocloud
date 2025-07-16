package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.kubernetes.persistence.KubernetesPvcHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class KubernetesPvcUpdateHandler implements ResourceActionHandler {

    private final KubernetesPvcHandler pvcHandler;

    public KubernetesPvcUpdateHandler(KubernetesPvcHandler pvcHandler) {
        this.pvcHandler = pvcHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pvcHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新PVC";
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
        return KubernetesPvcUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1PersistentVolumeClaim> pvc = pvcHandler.describePersistentVolumeClaim(account, resource.getExternalId());

        if(pvc.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesPvcUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(pvc.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updatePv(resource, parameters, false);
    }

    private void updatePv(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesPvcUpdateInput input = JSON.convert(parameters, KubernetesPvcUpdateInput.class);

        Resource namespace = resource.getEssentialTargetByType(KubernetesNamespaceHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("Namespace not found when updating PVC")
        );

        V1PersistentVolumeClaim pvc = KubeUtil.fromYaml(input.getYamlContent(), V1PersistentVolumeClaim.class);

        KubernetesProvider provider = (KubernetesProvider) pvcHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updatePersistentVolumeClaim(
                namespace.getExternalId(), pvc, dryRun
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
