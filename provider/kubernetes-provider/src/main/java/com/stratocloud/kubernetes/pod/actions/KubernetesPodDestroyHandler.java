package com.stratocloud.kubernetes.pod.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.pod.KubernetesPodHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPodDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesPodHandler podHandler;

    public KubernetesPodDestroyHandler(KubernetesPodHandler podHandler) {
        this.podHandler = podHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return podHandler;
    }

    @Override
    public String getTaskName() {
        return "删除Pod";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deletePod(resource, false);
    }

    private void deletePod(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) podHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1Pod> pod = podHandler.describePod(account, resource.getExternalId());

        if(pod.isEmpty())
            return;

        provider.buildClient(account).deletePod(KubeUtil.getNamespacedRef(pod.get().getMetadata()), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deletePod(resource, true);
    }
}
