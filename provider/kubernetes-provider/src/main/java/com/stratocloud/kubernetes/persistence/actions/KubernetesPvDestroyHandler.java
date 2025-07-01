package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.persistence.KubernetesPvHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPvDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesPvHandler pvHandler;

    public KubernetesPvDestroyHandler(KubernetesPvHandler pvHandler) {
        this.pvHandler = pvHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pvHandler;
    }

    @Override
    public String getTaskName() {
        return "删除PersistentVolume";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deletePv(resource, false);
    }

    private void deletePv(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) pvHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1PersistentVolume> pv = pvHandler.describePersistentVolume(account, resource.getExternalId());

        if(pv.isEmpty())
            return;

        provider.buildClient(account).deletePersistentVolume(
                KubeUtil.getObjectName(pv.get().getMetadata()), dryRun
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deletePv(resource, true);
    }
}
