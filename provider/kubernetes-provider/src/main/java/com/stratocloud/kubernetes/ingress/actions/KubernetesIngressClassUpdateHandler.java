package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.ingress.KubernetesIngressClassHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1IngressClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class KubernetesIngressClassUpdateHandler implements ResourceActionHandler {

    private final KubernetesIngressClassHandler ingressClassHandler;

    public KubernetesIngressClassUpdateHandler(KubernetesIngressClassHandler ingressClassHandler) {
        this.ingressClassHandler = ingressClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ingressClassHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新IngressClass";
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
        return KubernetesIngressClassUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1IngressClass> ingressClass = ingressClassHandler.describeIngressClass(account, resource.getExternalId());

        if(ingressClass.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(
                KubernetesIngressClassUpdateInput.class
        );

        String yamlContent = KubeUtil.toYaml(ingressClass.get());

        DynamicFormMetaData result = KubeUtil.replaceYamlContent(formMetaData, yamlContent);

        return Optional.of(result);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        updateIngress(resource, parameters, false);
    }

    private void updateIngress(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesIngressClassUpdateInput input = JSON.convert(parameters, KubernetesIngressClassUpdateInput.class);

        V1IngressClass ingressClass = KubeUtil.fromYaml(input.getYamlContent(), V1IngressClass.class);

        KubernetesProvider provider = (KubernetesProvider) ingressClassHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        provider.buildClient(account).updateIngressClass(
                ingressClass, dryRun
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
