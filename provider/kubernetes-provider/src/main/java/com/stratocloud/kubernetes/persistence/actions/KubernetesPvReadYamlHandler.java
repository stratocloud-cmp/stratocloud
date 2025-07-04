package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.persistence.KubernetesPvHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesPvReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesPvHandler pvHandler;

    public KubernetesPvReadYamlHandler(KubernetesPvHandler pvHandler) {
        this.pvHandler = pvHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pvHandler;
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
        Optional<V1PersistentVolume> pv = pvHandler.describePersistentVolume(
                account, resource.getExternalId()
        );

        if(pv.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "PersistentVolume Yaml",
                KubeUtil.toYaml(pv.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
