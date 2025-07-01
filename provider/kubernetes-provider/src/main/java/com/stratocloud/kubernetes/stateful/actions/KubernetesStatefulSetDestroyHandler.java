package com.stratocloud.kubernetes.stateful.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.stateful.KubernetesStatefulSetHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesStatefulSetDestroyHandler implements DestroyResourceActionHandler {

    private final KubernetesStatefulSetHandler statefulSetHandler;

    public KubernetesStatefulSetDestroyHandler(KubernetesStatefulSetHandler statefulSetHandler) {
        this.statefulSetHandler = statefulSetHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return statefulSetHandler;
    }

    @Override
    public String getTaskName() {
        return "删除StatefulSet";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        deleteStatefulSet(resource, false);
    }

    private void deleteStatefulSet(Resource resource, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) statefulSetHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<V1StatefulSet> statefulSet = statefulSetHandler.describeStatefulSet(account, resource.getExternalId());

        if(statefulSet.isEmpty())
            return;

        provider.buildClient(account).deleteStatefulSet(KubeUtil.getNamespacedRef(statefulSet.get().getMetadata()), dryRun);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        deleteStatefulSet(resource, true);
    }
}
