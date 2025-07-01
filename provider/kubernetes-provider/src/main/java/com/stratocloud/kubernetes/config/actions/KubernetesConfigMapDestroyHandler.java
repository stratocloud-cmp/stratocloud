package com.stratocloud.kubernetes.config.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.config.KubernetesConfigMapHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesConfigMapDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesConfigMapHandler configMapHandler;

    public KubernetesConfigMapDestroyHandler(KubernetesConfigMapHandler configMapHandler) {
        this.configMapHandler = configMapHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return configMapHandler;
    }

    @Override
    public String getTaskName() {
        return "删除ConfigMap";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteConfigMap(resource, false);
    }

    private void deleteConfigMap(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) configMapHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1ConfigMap> configMap = configMapHandler.describeConfigMap(account, resource.getExternalId());

        if(configMap.isEmpty())
            return;

        provider.buildClient(account).deleteConfigMap(
                KubeUtil.getNamespacedRef(configMap.get().getMetadata()),
                dryRun
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteConfigMap(resource, true);
    }
}
