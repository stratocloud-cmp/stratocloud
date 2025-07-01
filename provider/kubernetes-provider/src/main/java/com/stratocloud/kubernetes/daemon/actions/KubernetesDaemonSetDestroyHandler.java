package com.stratocloud.kubernetes.daemon.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.daemon.KubernetesDaemonSetHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesDaemonSetDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesDaemonSetHandler daemonSetHandler;

    public KubernetesDaemonSetDestroyHandler(KubernetesDaemonSetHandler daemonSetHandler) {
        this.daemonSetHandler = daemonSetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return daemonSetHandler;
    }

    @Override
    public String getTaskName() {
        return "删除DaemonSet";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteDaemonSet(resource, false);
    }

    private void deleteDaemonSet(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) daemonSetHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1DaemonSet> daemonSet = daemonSetHandler.describeDaemonSet(
                account, resource.getExternalId()
        );

        if(daemonSet.isEmpty())
            return;

        provider.buildClient(account).deleteDaemonSet(
                KubeUtil.getNamespacedRef(daemonSet.get().getMetadata()), dryRun
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteDaemonSet(resource, true);
    }
}
