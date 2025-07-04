package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.persistence.KubernetesStorageClassHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1StorageClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesStorageClassReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesStorageClassHandler storageClassHandler;

    public KubernetesStorageClassReadYamlHandler(KubernetesStorageClassHandler storageClassHandler) {
        this.storageClassHandler = storageClassHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return storageClassHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.READ_YAML;
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public List<ResourceReadActionResult> performReadAction(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<V1StorageClass> storageClass = storageClassHandler.describeStorageClass(
                account, resource.getExternalId()
        );

        if(storageClass.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "StorageClass Yaml",
                KubeUtil.toYaml(storageClass.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
