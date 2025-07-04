package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.persistence.KubernetesPvcHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.ResourceReadActionHandler;
import com.stratocloud.resource.*;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class KubernetesPvcReadYamlHandler implements ResourceReadActionHandler {

    private final KubernetesPvcHandler pvcHandler;

    public KubernetesPvcReadYamlHandler(KubernetesPvcHandler pvcHandler) {
        this.pvcHandler = pvcHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pvcHandler;
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
        Optional<V1PersistentVolumeClaim> pvc = pvcHandler.describePersistentVolumeClaim(
                account, resource.getExternalId()
        );

        if(pvc.isEmpty())
            return List.of();

        ResourceReadActionResult result = new ResourceReadActionResult(
                "PersistentVolumeClaim Yaml",
                KubeUtil.toYaml(pvc.get()),
                false,
                ResourceReadActionResult.ResultType.YAML
        );

        return List.of(result);
    }
}
