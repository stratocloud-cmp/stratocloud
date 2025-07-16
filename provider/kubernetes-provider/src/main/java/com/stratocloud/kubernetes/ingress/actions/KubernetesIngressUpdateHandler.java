package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.ingress.KubernetesIngressHandler;
import com.stratocloud.kubernetes.namespace.KubernetesNamespaceHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Ingress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class KubernetesIngressUpdateHandler implements ResourceActionHandler {

    private final KubernetesIngressHandler ingressHandler;

    public KubernetesIngressUpdateHandler(KubernetesIngressHandler ingressHandler) {
        this.ingressHandler = ingressHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ingressHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新Ingress";
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
        return KubernetesIngressUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1Ingress> ingress = ingressHandler.describeIngress(account, resource.getExternalId());

        if(ingress.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesIngressUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(ingress.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updateIngress(resource, parameters, false);
    }

    private void updateIngress(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesIngressUpdateInput input = JSON.convert(parameters, KubernetesIngressUpdateInput.class);

        Resource namespace = resource.getEssentialTargetByType(KubernetesNamespaceHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("Namespace not found when updating ingress")
        );

        V1Ingress ingress = KubeUtil.fromYaml(input.getYamlContent(), V1Ingress.class);

        KubernetesProvider provider = (KubernetesProvider) ingressHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updateIngress(
                namespace.getExternalId(), ingress, dryRun
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
        updateIngress(resource, parameters, true);
    }
}
