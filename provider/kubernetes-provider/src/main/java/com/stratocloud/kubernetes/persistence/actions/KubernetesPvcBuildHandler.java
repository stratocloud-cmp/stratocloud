package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.persistence.KubernetesPvcHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KubernetesPvcBuildHandler implements BuildResourceActionHandler {

    private final KubernetesPvcHandler pvcHandler;

    public KubernetesPvcBuildHandler(KubernetesPvcHandler pvcHandler) {
        this.pvcHandler = pvcHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pvcHandler;
    }

    @Override
    public String getTaskName() {
        return "创建PersistentVolumeClaim";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return KubernetesPvcBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        createPvc(resource, parameters, false);
    }

    private void createPvc(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        KubernetesProvider provider = (KubernetesProvider) pvcHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var input = JSON.convert(parameters, KubernetesPvcBuildInput.class);

        Resource namespace = resource.getEssentialTarget(ResourceCategories.NAMESPACE).orElseThrow(
                () -> new StratoException("Namespace not found when creating pvc")
        );

        V1PersistentVolumeClaim pvc = KubeUtil.fromYaml(input.getYamlContent(), V1PersistentVolumeClaim.class);
        V1PersistentVolumeClaim result = provider.buildClient(account).createPersistentVolumeClaim(
                namespace.getExternalId(), pvc, dryRun
        );

        resource.setExternalId(KubeUtil.getObjectName(result.getMetadata()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createPvc(resource, parameters, true);
    }
}
