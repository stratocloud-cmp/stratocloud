package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.persistence.KubernetesStorageClassHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1StorageClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class KubernetesStorageClassUpdateHandler implements ResourceActionHandler {

    private final KubernetesStorageClassHandler storageClassHandler;

    public KubernetesStorageClassUpdateHandler(KubernetesStorageClassHandler storageClassHandler) {
        this.storageClassHandler = storageClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return storageClassHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新StorageClass";
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
        return KubernetesStorageClassUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1StorageClass> storageClass = storageClassHandler.describeStorageClass(
                account, resource.getExternalId()
        );

        if(storageClass.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesStorageClassUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(storageClass.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updateStorageClass(resource, parameters, false);
    }

    private void updateStorageClass(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesStorageClassUpdateInput input = JSON.convert(parameters, KubernetesStorageClassUpdateInput.class);

        V1StorageClass storageClass = KubeUtil.fromYaml(input.getYamlContent(), V1StorageClass.class);

        KubernetesProvider provider = (KubernetesProvider) storageClassHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updateStorageClass(
                storageClass, dryRun
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
        updateStorageClass(resource, parameters, true);
    }
}
