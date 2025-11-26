package com.stratocloud.provider.huawei.kafka;

import com.huaweicloud.sdk.kafka.v2.model.ListInstancesRequest;
import com.huaweicloud.sdk.kafka.v2.model.ShowInstanceResp;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class HuaweiKafkaHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiKafkaHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_KAFKA";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云Kafka实例";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.MQ_INSTANCE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<ShowInstanceResp> instance = describeInstance(account, externalId);
        return instance.map(
                i->toExternalResource(account, i)
        );
    }

    public ExternalResource toExternalResource(ExternalAccount account, ShowInstanceResp instance) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                instance.getInstanceId(),
                instance.getName(),
                convertStatus(instance.getStatus())
        );
    }
    private ResourceState convertStatus(String status) {
        if(Utils.isBlank(status))
            return ResourceState.UNKNOWN;

        return switch (status) {
            case "CREATING" -> ResourceState.BUILDING;
            case "STARTING" -> ResourceState.STARTING;
            case "RUNNING" -> ResourceState.STARTED;
            case "ERROR", "EXTENDEDFAILED" -> ResourceState.ERROR;
            case "RESTARTING" -> ResourceState.RESTARTING;
            case "FROZEN" -> ResourceState.DISABLED;
            case "EXTENDING", "ROLLINGBACK", "UPGRADING", "FREEZING" -> ResourceState.CONFIGURING;
            case "DELETING" -> ResourceState.DESTROYING;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<ShowInstanceResp> describeInstance(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).kafka().describeInstance(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        ListInstancesRequest request = new ListInstancesRequest();
        return provider.buildClient(account).kafka().describeInstances(request).stream().map(
                i -> toExternalResource(account,i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var instance = describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Kafka instance not found")
        );

        resource.updateByExternal(toExternalResource(account, instance));

        RuntimeProperty typeProperty = RuntimeProperty.ofDisplayInList(
                "type",
                "实例类型",
                instance.getType().getValue(),
                Objects.equals(instance.getType().getValue(), "cluster") ? "集群" : "单机"
        );
        resource.addOrUpdateRuntimeProperty(typeProperty);

        RuntimeProperty storageProperty = RuntimeProperty.ofDisplayInList(
                "storageGb",
                "存储空间(GB)",
                String.valueOf(instance.getStorageSpace()),
                String.valueOf(instance.getStorageSpace())
        );
        resource.addOrUpdateRuntimeProperty(storageProperty);

        RuntimeProperty connectAddressProperty = RuntimeProperty.ofDisplayInList(
                "connectAddress",
                "连接地址",
                instance.getConnectAddress(),
                instance.getConnectAddress()
        );
        resource.addOrUpdateRuntimeProperty(connectAddressProperty);

        resource.updateUsageByType(
                UsageTypes.DISK_GB,
                new BigDecimal(instance.getStorageSpace())
        );


    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.DISK_GB
        );
    }
}
