package com.stratocloud.kubernetes.config.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.config.KubernetesSecretHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1Secret;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesSecretBuildHandler implements BuildResourceActionHandler {

    private final KubernetesSecretHandler secretHandler;

    public KubernetesSecretBuildHandler(KubernetesSecretHandler secretHandler) {
        this.secretHandler = secretHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return secretHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Secret";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesSecretBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createSecret(resource, parameters, false);
    }

    private void createSecret(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) secretHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesSecretBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating secret")
        );

        V1Secret secret = KubeUtil.fromYaml(input.getYamlContent(), V1Secret.class);
        V1Secret result = provider.buildClient(account).createSecret(
                namespace.getExternalId(), secret, dryRun
        );

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createSecret(resource, parameters, true);
    }
}
