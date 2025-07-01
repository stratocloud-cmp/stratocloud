package com.stratocloud.kubernetes.config.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.config.KubernetesSecretHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1Secret;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesSecretDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesSecretHandler secretHandler;

    public KubernetesSecretDestroyHandler(KubernetesSecretHandler secretHandler) {
        this.secretHandler = secretHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return secretHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Secret";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteSecret(resource, false);
    }

    private void deleteSecret(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) secretHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1Secret> secret = secretHandler.describeSecret(account, resource.getExternalId());

        if(secret.isEmpty())
            return;

        provider.buildClient(account).deleteSecret(
                KubeUtil.getNamespacedRef(secret.get().getMetadata()),
                dryRun
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteSecret(resource, true);
    }
}
