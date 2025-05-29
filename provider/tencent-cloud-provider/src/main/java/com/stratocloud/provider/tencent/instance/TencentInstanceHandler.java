package com.stratocloud.provider.tencent.instance;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutorFactory;
import com.stratocloud.provider.resource.event.EventAwareResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentEventTypes;
import com.stratocloud.provider.tencent.common.TencentTimeUtil;
import com.stratocloud.provider.tencent.instance.command.TencentPowerShellCommandExecutorFactory;
import com.stratocloud.provider.tencent.instance.command.TencentShellCommandExecutorFactory;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cloudaudit.v20190319.models.Event;
import com.tencentcloudapi.cvm.v20170312.models.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TencentInstanceHandler extends AbstractResourceHandler
        implements GuestOsHandler, EventAwareResourceHandler {

    private final TencentCloudProvider provider;

    private final TencentShellCommandExecutorFactory shellCommandExecutorFactory;

    private final TencentPowerShellCommandExecutorFactory powerShellCommandExecutorFactory;

    public TencentInstanceHandler(TencentCloudProvider provider,
                                  TencentShellCommandExecutorFactory shellCommandExecutorFactory,
                                  TencentPowerShellCommandExecutorFactory powerShellCommandExecutorFactory) {
        this.provider = provider;
        this.shellCommandExecutorFactory = shellCommandExecutorFactory;
        this.powerShellCommandExecutorFactory = powerShellCommandExecutorFactory;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_INSTANCE";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云云主机";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.COMPUTE_INSTANCE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<Instance> instance = describeInstance(account, externalId);

        return instance.map(i -> toExternalResource(account, i));
    }

    private ExternalResource toExternalResource(ExternalAccount account, Instance instance) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                instance.getInstanceId(),
                instance.getInstanceName(),
                convertState(instance.getInstanceState())
        );
    }


    private ResourceState convertState(String instanceState) {
        return switch (instanceState){
            case "PENDING" -> ResourceState.BUILDING;
            case "LAUNCH_FAILED" -> ResourceState.BUILD_ERROR;
            case "RUNNING" -> ResourceState.STARTED;
            case "STOPPED" -> ResourceState.STOPPED;
            case "STARTING" -> ResourceState.STARTING;
            case "STOPPING" -> ResourceState.STOPPING;
            case "REBOOTING" -> ResourceState.RESTARTING;
            case "SHUTDOWN" -> ResourceState.SHUTDOWN;
            case "TERMINATING" -> ResourceState.DESTROYING;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<Instance> describeInstance(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeInstance(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account,
                                                            Map<String, Object> queryArgs) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();

        return provider.buildClient(account).describeInstances(request).stream().map(
                instance -> toExternalResource(account, instance)
        ).toList();
    }

    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<Instance> instance = describeInstance(account, externalResource.externalId());

        if(instance.isEmpty())
            return List.of();

        if(instance.get().getTags() == null)
            return List.of();

        return Arrays.stream(instance.get().getTags()).map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Instance instance = describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Instance not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, instance));

        String flavor = "%sC%sG".formatted(instance.getCPU(), instance.getMemory());
        RuntimeProperty flavorProperty = RuntimeProperty.ofDisplayInList(
                "flavor", "规格", flavor, flavor
        );

        String systemDiskSize = instance.getSystemDisk().getDiskSize().toString();
        RuntimeProperty systemDiskSizeProperty = RuntimeProperty.ofDisplayable(
                "systemDiskSize", "系统盘大小(GB)", systemDiskSize, systemDiskSize
        );

        String dataDisksSize = String.valueOf(getTotalDataDisksSize(instance.getDataDisks()));
        RuntimeProperty dataDisksSizeProperty = RuntimeProperty.ofDisplayable(
                "dataDisksSize", "数据盘总大小(GB)", dataDisksSize, dataDisksSize
        );


        String privateIps = instance.getPrivateIpAddresses() != null ?
                String.join(",", instance.getPrivateIpAddresses()) : "";
        RuntimeProperty privateIpsProperty = RuntimeProperty.ofDisplayInList(
                "privateIps", "内网IP", privateIps, privateIps
        );

        String publicIps = instance.getPublicIpAddresses() != null ?
                String.join(",", instance.getPublicIpAddresses()) : "";
        RuntimeProperty publicIpsProperty = RuntimeProperty.ofDisplayInList(
                "publicIps", "公网IP", publicIps, publicIps
        );

        String ipv6Ips = instance.getIPv6Addresses() != null ?
                String.join(",", instance.getIPv6Addresses()) : "";
        RuntimeProperty ipv6IpsProperty = RuntimeProperty.ofDisplayable(
                "ipv6Ips", "IPv6地址", ipv6Ips, ipv6Ips
        );

        String gpuType = instance.getGPUInfo() != null ? instance.getGPUInfo().getGPUType() : "";
        RuntimeProperty gpuTypeProperty = RuntimeProperty.ofDisplayable(
                "gpuType", "GPU型号", gpuType, gpuType
        );

        resource.addOrUpdateRuntimeProperty(flavorProperty);
        resource.addOrUpdateRuntimeProperty(systemDiskSizeProperty);
        resource.addOrUpdateRuntimeProperty(dataDisksSizeProperty);
        resource.addOrUpdateRuntimeProperty(privateIpsProperty);
        resource.addOrUpdateRuntimeProperty(publicIpsProperty);
        resource.addOrUpdateRuntimeProperty(ipv6IpsProperty);
        resource.addOrUpdateRuntimeProperty(gpuTypeProperty);

        resource.updateUsageByType(UsageTypes.CPU_CORES, BigDecimal.valueOf(instance.getCPU()));
        resource.updateUsageByType(UsageTypes.MEMORY_GB, BigDecimal.valueOf(instance.getMemory()));

        RuntimePropertiesUtil.autoSyncGuestManagementInfoQuietly(resource);

        if(Utils.isNotEmpty(instance.getPublicIpAddresses()))
            RuntimePropertiesUtil.setManagementIp(resource, instance.getPublicIpAddresses()[0]);
        else if(Utils.isNotEmpty(instance.getPrivateIpAddresses()))
            RuntimePropertiesUtil.setManagementIp(resource, instance.getPrivateIpAddresses()[0]);
    }

    private long getTotalDataDisksSize(DataDisk[] dataDisks) {
        long sum = 0L;
        if(Utils.isEmpty(dataDisks))
            return sum;
        for (DataDisk dataDisk : dataDisks) {
            sum = sum + dataDisk.getDiskSize();
        }
        return sum;
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(UsageTypes.CPU_CORES, UsageTypes.MEMORY_GB);
    }


    @Override
    public ResourceCost getCurrentCost(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return ResourceCost.ZERO;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Instance instance = client.describeInstance(resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Instance not found.")
        );


        switch (instance.getInstanceChargeType()){
            case "PREPAID" -> {
                LocalDateTime createdTime = TencentTimeUtil.toLocalDateTime(instance.getCreatedTime());
                LocalDateTime expiredTime = TencentTimeUtil.toLocalDateTime(instance.getExpiredTime());

                long months = ChronoUnit.MONTHS.between(createdTime, expiredTime);

                var request = new InquiryPriceRenewInstancesRequest();
                request.setInstanceIds(new String[]{instance.getInstanceId()});
                InstanceChargePrepaid prepaid = new InstanceChargePrepaid();
                prepaid.setPeriod(months);
                request.setInstanceChargePrepaid(prepaid);
                request.setRenewPortableDataDisk(false);

                ItemPrice instancePrice = client.inquiryPriceRenewInstance(request).getInstancePrice();
                return new ResourceCost(instancePrice.getDiscountPrice(), months, ChronoUnit.MONTHS);
            }
            case "POSTPAID_BY_HOUR", "SPOTPAID" -> {
                var request = new InquiryPriceResetInstanceRequest();
                request.setInstanceId(instance.getInstanceId());
                ItemPrice instancePrice = client.inquiryPriceResetInstance(request).getInstancePrice();
                return new ResourceCost(instancePrice.getUnitPriceDiscount(), 1.0, ChronoUnit.HOURS);
            }
            default -> {
                return ResourceCost.ZERO;
            }
        }
    }


    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }

    @Override
    public OsType getOsType(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<Instance> instance = describeInstance(account, resource.getExternalId());
        if(instance.isEmpty())
            return OsType.Unknown;

        String osName = instance.get().getOsName();

        if(Utils.isNotBlank(osName) && osName.toLowerCase().contains("windows"))
            return OsType.Windows;

        return OsType.Linux;
    }

    @Override
    public List<ProviderGuestCommandExecutorFactory> getProviderCommandExecutorFactories(Resource resource) {
        OsType osType = getOsTypeQuietly(resource);

        if(osType == OsType.Linux)
            return List.of(shellCommandExecutorFactory);
        else if(osType == OsType.Windows)
            return List.of(powerShellCommandExecutorFactory);
        else
            return List.of();
    }

    @Override
    public List<ExternalResourceEvent> describeResourceEvents(ExternalAccount account,
                                                              String externalId,
                                                              LocalDateTime happenedAfter) {
        TencentCloudClient client = provider.buildClient(account);

        List<String> eventNames = TencentEventTypes.instanceEventTypes.stream().map(
                TencentEventTypes.TencentEventType::externalEventName
        ).toList();

        List<Event> events = client.describeEvents(eventNames, "cvm", externalId, happenedAfter);

        List<ExternalResourceEvent> result = new ArrayList<>();

        for (Event event : events) {
            var tencentEventType = TencentEventTypes.fromInstanceEventName(event.getEventName());

            if(tencentEventType.isEmpty())
                continue;

            ExternalResourceEvent externalResourceEvent = new ExternalResourceEvent(
                    event.getRequestID(),
                    tencentEventType.get().eventType(),
                    StratoEventLevel.INFO,
                    StratoEventSource.EXTERNAL_ACTION,
                    getResourceTypeId(),
                    account.getId(),
                    externalId,
                    event.getEventNameCn(),
                    TencentTimeUtil.fromEpochSeconds(event.getEventTime())
            );
            result.add(
                    externalResourceEvent
            );
        }

        return result;
    }
}
