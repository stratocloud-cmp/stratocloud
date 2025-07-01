package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.persistence.KubernetesPvcHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPvcDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesPvcHandler pvcHandler;

    public KubernetesPvcDestroyHandler(KubernetesPvcHandler pvcHandler) {
        this.pvcHandler = pvcHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pvcHandler;
    }

    @Override
    public String getTaskName() {
        return "删除PersistentVolumeClaim";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deletePvc(resource, false);
    }

    private void deletePvc(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) pvcHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1PersistentVolumeClaim> pvc = pvcHandler.describePersistentVolumeClaim(
                account, resource.getExternalId()
        );

        if(pvc.isEmpty())
            return;

        provider.buildClient(account).deletePersistentVolumeClaim(KubeUtil.getNamespacedRef(pvc.get().getMetadata()), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deletePvc(resource, true);
    }
}
