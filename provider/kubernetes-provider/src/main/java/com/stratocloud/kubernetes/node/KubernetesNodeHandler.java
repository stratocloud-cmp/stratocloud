package com.stratocloud.kubernetes.node;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeAddress;
import io.kubernetes.client.openapi.models.V1NodeStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesNodeHandler extends AbstractResourceHandler {

    private final KubernetesProvider provider;

    public KubernetesNodeHandler(KubernetesProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "KUBERNETES_NODE";
    }

    @Override
    public String getResourceTypeName() {
        return "K8s Node";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.NODE;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeNode(account, externalId).map(
                n -> toExternalResource(account, n)
        );
    }

    public Optional<V1Node> describeNode(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeNode(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, V1Node node) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                KubeUtil.getObjectName(node.getMetadata()),
                KubeUtil.getObjectName(node.getMetadata()),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeNodes().stream().map(
                n -> toExternalResource(account, n)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        V1Node node = describeNode(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Node not found")
        );

        resource.updateByExternal(toExternalResource(account, node));

        V1NodeStatus status = node.getStatus();
        if(status != null){
            Map<String, Quantity> capacity = status.getCapacity();

            if(Utils.isNotEmpty(capacity)){
                Quantity cpuQuantity = capacity.get("cpu");
                if(cpuQuantity != null){
                    String cpuCapacity = cpuQuantity.getNumber().setScale(
                            2, RoundingMode.FLOOR
                    ).toPlainString();

                    RuntimeProperty cpuProperty = RuntimeProperty.ofDisplayInList(
                            "cpuCapacity",
                            "CPU容量(核)",
                            cpuCapacity,
                            cpuCapacity
                    );
                    resource.addOrUpdateRuntimeProperty(cpuProperty);
                }

                Quantity memoryQuantity = capacity.get("memory");
                if(memoryQuantity != null){
                    String memoryCapacity = memoryQuantity.getNumber().divide(
                            BigDecimal.valueOf(2L).pow(20), RoundingMode.FLOOR
                    ).setScale(
                            2, RoundingMode.FLOOR
                    ).toPlainString();

                    RuntimeProperty memoryProperty = RuntimeProperty.ofDisplayInList(
                            "memoryCapacity",
                            "内存容量(MiB)",
                            memoryCapacity,
                            memoryCapacity
                    );
                    resource.addOrUpdateRuntimeProperty(memoryProperty);
                }

                Quantity storageQuantity = capacity.get("ephemeral-storage");
                if(storageQuantity != null){
                    String storageCapacity = storageQuantity.getNumber().divide(
                            BigDecimal.valueOf(2L).pow(30), RoundingMode.FLOOR
                    ).setScale(2, RoundingMode.FLOOR).toPlainString();

                    RuntimeProperty storageProperty = RuntimeProperty.ofDisplayInList(
                            "storageCapacity",
                            "存储容量(GiB)",
                            storageCapacity,
                            storageCapacity
                    );
                    resource.addOrUpdateRuntimeProperty(storageProperty);
                }
            }

            List<V1NodeAddress> addresses = status.getAddresses();

            if(Utils.isNotEmpty(addresses)){
                String addressStr = String.join(
                        ",",
                        addresses.stream().map(V1NodeAddress::getAddress).toList()
                );

                RuntimeProperty addressProperty = RuntimeProperty.ofDisplayInList(
                        "addresses",
                        "节点地址",
                        addressStr,
                        addressStr
                );

                resource.addOrUpdateRuntimeProperty(addressProperty);
            }
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
