package com.stratocloud.kubernetes.pod;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.event.EventAwareResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPodHandler extends AbstractResourceHandler implements EventAwareResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesPodHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_POD";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s Pod";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.POD;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describePod(account, externalId).map(
                i -> toExternalResource(account, i)
        );
    }

    public Optional<V1Pod> describePod(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describePod(NamespacedRef.fromString(externalId));
    }

    public ExternalResource toExternalResource(ExternalAccount account, V1Pod pod) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getNamespacedRef(pod.getMetadata()).toString(),
                KubeUtil.getObjectName(pod.getMetadata()),
                convertState(pod)
        );
    }

    private ResourceState convertState(V1Pod pod) {
        V1PodStatus status = pod.getStatus();

        if(status == null)
            return ResourceState.UNKNOWN;

        String phase = status.getPhase();

        if(phase == null)
            return ResourceState.UNKNOWN;

        return switch (phase){
            case "Pending" -> ResourceState.STARTING;
            case "Running" -> ResourceState.STARTED;
            case "Succeeded" -> ResourceState.STOPPED;
            case "Failed" -> ResourceState.ERROR;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describePods().stream().map(
                p -> toExternalResource(account, p)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        V1Pod pod = describePod(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Pod not found")
        );

        resource.updateByExternal(toExternalResource(account, pod));

        V1PodStatus status = pod.getStatus();

        if(status != null){
            if(Utils.isNotBlank(status.getPodIP())){
                RuntimeProperty podIpProperty = RuntimeProperty.ofDisplayInList(
                        "podIp",
                        "Pod IP",
                        status.getPodIP(),
                        status.getPodIP()
                );

                resource.addOrUpdateRuntimeProperty(podIpProperty);
            }
        }

        V1PodSpec spec = pod.getSpec();

        if(spec != null){
            if(spec.getResources() != null && Utils.isNotEmpty(spec.getResources().getLimits())){
                Map<String, Quantity> limits = spec.getResources().getLimits();

                Quantity cpuQuantity = limits.get("cpu");

                if(cpuQuantity != null){
                    String cpuLimit = cpuQuantity.getNumber().setScale(2, RoundingMode.FLOOR).toPlainString();
                    RuntimeProperty cpuLimitProperty = RuntimeProperty.ofDisplayInList(
                            "cpuLimit",
                            "CPU上限(核)",
                            cpuLimit,
                            cpuLimit
                    );
                    resource.addOrUpdateRuntimeProperty(cpuLimitProperty);
                }

                Quantity memoryQuantity = limits.get("memory");

                if(memoryQuantity != null){
                    String memoryLimit = memoryQuantity.getNumber().divide(
                            BigDecimal.valueOf(2L).pow(20), RoundingMode.FLOOR
                    ).setScale(2, RoundingMode.FLOOR).toPlainString();
                    RuntimeProperty memoryLimitProperty = RuntimeProperty.ofDisplayInList(
                            "memoryLimit",
                            "内存上限(MiB)",
                            memoryLimit,
                            memoryLimit
                    );
                    resource.addOrUpdateRuntimeProperty(memoryLimitProperty);
                }
            }
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    @Override
    public List<ExternalResourceEvent> describeResourceEvents(ExternalAccount account,
                                                              String externalId,
                                                              LocalDateTime happenedAfter) {
        return KubeUtil.describeResourceEvents(
                provider,
                account,
                "Pod",
                getResourceTypeId(),
                externalId,
                happenedAfter,
                true
        );
    }
}
