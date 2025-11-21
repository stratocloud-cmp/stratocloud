package com.stratocloud.provider.huawei.dcs;

import com.huaweicloud.sdk.dcs.v2.model.InstanceListInfo;
import com.huaweicloud.sdk.dcs.v2.model.ListInstancesRequest;
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
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiDcsHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiDcsHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_DCS";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云Redis实例";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.NOSQL_DB_INSTANCE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<InstanceListInfo> instance = describeInstance(account, externalId);
        return instance.map(
                i->toExternalResource(account, i)
        );
    }

    public ExternalResource toExternalResource(ExternalAccount account, InstanceListInfo instance) {
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
            case "CREATEFAILED" -> ResourceState.BUILD_ERROR;
            case "RUNNING" -> ResourceState.STARTED;
            case "ERROR" -> ResourceState.ERROR;
            case "RESTARTING" -> ResourceState.RESTARTING;
            case "FROZEN" -> ResourceState.DISABLED;
            case "EXTENDING", "RESTORING", "FLUSHING" -> ResourceState.CONFIGURING;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<InstanceListInfo> describeInstance(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).dcs().describeInstance(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        ListInstancesRequest request = new ListInstancesRequest();
        return provider.buildClient(account).dcs().describeInstances(request).stream().map(
                i -> toExternalResource(account,i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var instance = describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("DCS instance not found")
        );

        resource.updateByExternal(toExternalResource(account, instance));

        BigDecimal memoryGb = BigDecimal.valueOf(instance.getMaxMemory() / 1024.0);
        String memoryGbStr = memoryGb.setScale(3, RoundingMode.HALF_UP).toPlainString();

        RuntimeProperty memoryProperty = RuntimeProperty.ofDisplayInList(
                "memoryGb",
                "内存大小(GB)",
                memoryGbStr,
                memoryGbStr
        );

        resource.addOrUpdateRuntimeProperty(memoryProperty);

        RuntimeProperty ipProperty = RuntimeProperty.ofDisplayInList(
                "ip",
                "内网IP",
                instance.getIp(),
                instance.getIp()
        );
        resource.addOrUpdateRuntimeProperty(ipProperty);

        if(instance.getPort() != null){
            RuntimeProperty portProperty = RuntimeProperty.ofDisplayInList(
                    "port",
                    "端口",
                    String.valueOf(instance.getPort()),
                    String.valueOf(instance.getPort())
            );
            resource.addOrUpdateRuntimeProperty(portProperty);
        }

        if(Utils.isNotBlank(instance.getPublicipAddress())){
            RuntimeProperty publicIpProperty = RuntimeProperty.ofDisplayInList(
                    "publicIp",
                    "公网IP",
                    instance.getPublicipAddress(),
                    instance.getPublicipAddress()
            );
            resource.addOrUpdateRuntimeProperty(publicIpProperty);
        }

        BigDecimal usedMemoryGb = BigDecimal.valueOf(instance.getUsedMemory() / 1024.0);
        String usedMemoryGbStr = usedMemoryGb.setScale(3, RoundingMode.HALF_UP).toPlainString();

        RuntimeProperty usedMemoryProperty = RuntimeProperty.ofDisplayable(
                "usedMemoryGb",
                "已使用内存(GB)",
                usedMemoryGbStr,
                usedMemoryGbStr
        );

        resource.addOrUpdateRuntimeProperty(usedMemoryProperty);



        resource.updateUsageByType(UsageTypes.MEMORY_GB, memoryGb);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.MEMORY_GB
        );
    }
}
