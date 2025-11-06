package com.stratocloud.provider.huawei.rds;

import com.huaweicloud.sdk.rds.v3.model.InstanceResponse;
import com.huaweicloud.sdk.rds.v3.model.ListInstancesRequest;
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
import java.util.Optional;

@Component
public class HuaweiRdsHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiRdsHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_RDS";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云RDS实例";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.RELATIONAL_DB_INSTANCE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<InstanceResponse> instance = describeInstance(account, externalId);
        return instance.map(
                i->toExternalResource(account, i)
        );
    }

    public ExternalResource toExternalResource(ExternalAccount account, InstanceResponse instance) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                instance.getId(),
                instance.getName(),
                convertStatus(instance.getStatus())
        );
    }
    private ResourceState convertStatus(String status) {
        if(Utils.isBlank(status))
            return ResourceState.UNKNOWN;

        return switch (status) {
            case "BUILD" -> ResourceState.BUILDING;
            case "CREATE FAIL" -> ResourceState.BUILD_ERROR;
            case "ACTIVE" -> ResourceState.STARTED;
            case "FAILED" -> ResourceState.ERROR;
            case "FROZEN" -> ResourceState.DISABLED;
            case "MODIFYING", "RESTORING", "MODIFYING INSTANCE TYPE",
                 "SWITCHOVER", "MIGRATING", "BACKING UP", "MODIFYING DATABASE PORT" -> ResourceState.CONFIGURING;
            case "REBOOTING" -> ResourceState.RESTARTING;
            case "STORAGE FULL" -> ResourceState.INSUFFICIENT_RESOURCE;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<InstanceResponse> describeInstance(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).rds().describeInstance(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        ListInstancesRequest request = new ListInstancesRequest();
        return provider.buildClient(account).rds().describeInstances(request).stream().map(
                i -> toExternalResource(account,i)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        InstanceResponse instance = describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("RDS instance not found")
        );

        resource.updateByExternal(toExternalResource(account, instance));

        if(Utils.isNotEmpty(instance.getPrivateIps())){
            String privateIps = String.join(",", instance.getPrivateIps());

            RuntimeProperty privateIpProperty = RuntimeProperty.ofDisplayInList(
                    "privateIps",
                    "内网IP",
                    privateIps,
                    privateIps
            );
            resource.addOrUpdateRuntimeProperty(privateIpProperty);
        }

        if(Utils.isNotEmpty(instance.getPrivateDnsNames())){
            String privateDnsNames = String.join(",", instance.getPrivateDnsNames());

            RuntimeProperty privateDnsNamesProperty = RuntimeProperty.ofDisplayInList(
                    "privateDnsNames",
                    "内网域名",
                    privateDnsNames,
                    privateDnsNames
            );
            resource.addOrUpdateRuntimeProperty(privateDnsNamesProperty);
        }

        if(Utils.isNotEmpty(instance.getPublicIps())){
            String publicIps = String.join(",", instance.getPublicIps());

            RuntimeProperty publicIpProperty = RuntimeProperty.ofDisplayInList(
                    "publicIps",
                    "公网IP",
                    publicIps,
                    publicIps
            );
            resource.addOrUpdateRuntimeProperty(publicIpProperty);
        }

        if(Utils.isNotEmpty(instance.getPublicDnsNames())){
            String publicDnsNames = String.join(",", instance.getPublicDnsNames());

            RuntimeProperty publicDnsNamesProperty = RuntimeProperty.ofDisplayInList(
                    "publicDnsNames",
                    "公网域名",
                    publicDnsNames,
                    publicDnsNames
            );
            resource.addOrUpdateRuntimeProperty(publicDnsNamesProperty);
        }

        if(instance.getPort() != null){
            RuntimeProperty portProperty = RuntimeProperty.ofDisplayInList(
                    "port",
                    "端口",
                    String.valueOf(instance.getPort()),
                    String.valueOf(instance.getPort())
            );
            resource.addOrUpdateRuntimeProperty(portProperty);
        }

        String spec = "%sC%sG-%sG".formatted(instance.getCpu(), instance.getMem(), instance.getVolume().getSize());
        RuntimeProperty specProperty = RuntimeProperty.ofDisplayInList(
                "spec",
                "规格大小",
                spec,
                spec
        );
        resource.addOrUpdateRuntimeProperty(specProperty);

        resource.updateUsageByType(UsageTypes.CPU_CORES, new BigDecimal(instance.getCpu()));
        resource.updateUsageByType(UsageTypes.MEMORY_GB, new BigDecimal(instance.getMem()));
        resource.updateUsageByType(UsageTypes.DISK_GB, new BigDecimal(instance.getVolume().getSize()));
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.CPU_CORES,
                UsageTypes.MEMORY_GB,
                UsageTypes.DISK_GB
        );
    }
}
