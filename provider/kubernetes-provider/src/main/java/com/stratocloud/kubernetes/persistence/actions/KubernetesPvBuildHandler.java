package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.node.actions.KubernetesNodeBuildInput;
import com.stratocloud.kubernetes.persistence.KubernetesPvHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesPvBuildHandler implements BuildResourceActionHandler {

    private final KubernetesPvHandler pvHandler;

    public KubernetesPvBuildHandler(KubernetesPvHandler pvHandler) {
        this.pvHandler = pvHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pvHandler;
    }

    @Override
    public String getTaskName() {
        return "创建PersistentVolume";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesPvBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createPv(resource, parameters, false);
    }

    private void createPv(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) pvHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesNodeBuildInput.class);

        V1PersistentVolume node = KubeUtil.fromYaml(input.getYamlContent(), V1PersistentVolume.class);
        V1PersistentVolume result = provider.buildClient(account).createPersistentVolume(node, dryRun);

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createPv(resource, parameters, true);
    }
}
