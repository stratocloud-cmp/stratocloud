package com.stratocloud.provider.aliyun.instance;

import com.aliyun.ecs20140526.models.DescribeDisksRequest;
import com.aliyun.ecs20140526.models.DescribeInstancesRequest;
import com.aliyun.ecs20140526.models.DescribePriceRequest;
import com.aliyun.ecs20140526.models.DescribeRenewalPriceRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.AliyunEvent;
import com.stratocloud.provider.aliyun.common.AliyunEventTypes;
import com.stratocloud.provider.aliyun.common.AliyunTimeUtil;
import com.stratocloud.provider.aliyun.disk.AliyunDisk;
import com.stratocloud.provider.aliyun.instance.command.AliyunPowerShellCommandExecutorFactory;
import com.stratocloud.provider.aliyun.instance.command.AliyunShellCommandExecutorFactory;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.guest.command.ProviderGuestCommandExecutorFactory;
import com.stratocloud.provider.resource.event.EventAwareResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunInstanceHandler extends AbstractResourceHandler
        implements GuestOsHandler, EventAwareResourceHandler {

    private final AliyunCloudProvider provider;

    private final AliyunShellCommandExecutorFactory shellCommandExecutorFactory;

    private final AliyunPowerShellCommandExecutorFactory powerShellCommandExecutorFactory;

    public AliyunInstanceHandler(AliyunCloudProvider provider,
                                 AliyunShellCommandExecutorFactory shellCommandExecutorFactory,
                                 AliyunPowerShellCommandExecutorFactory powerShellCommandExecutorFactory) {
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
        return "ALIYUN_INSTANCE";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云云主机";
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
        Optional<AliyunInstance> instance = describeInstance(account, externalId);
        return instance.map(i -> toExternalResource(account, i));
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunInstance instance) {
        String instanceId = instance.detail().getInstanceId();
        String instanceName = instance.detail().getInstanceName();

        String resourceName = Utils.isNotBlank(instanceName) ? instanceName : instanceId;

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                instanceId,
                resourceName,
                convertState(instance.detail().getStatus())
        );
    }


    private ResourceState convertState(String status) {
        return switch (status){
            case "Pending" -> ResourceState.BUILDING;
            case "Running" -> ResourceState.STARTED;
            case "Stopped" -> ResourceState.STOPPED;
            case "Starting" -> ResourceState.STARTING;
            case "Stopping" -> ResourceState.STOPPING;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<AliyunInstance> describeInstance(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeInstance(externalId);
    }

    public List<AliyunDisk> describeInstanceDisks(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return List.of();

        DescribeDisksRequest request = new DescribeDisksRequest();
        request.setInstanceId(externalId);
        return provider.buildClient(account).ecs().describeDisks(request);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account,
                                                            Map<String, Object> queryArgs) {

        DescribeInstancesRequest request = new DescribeInstancesRequest();

        return provider.buildClient(account).ecs().describeInstances(request).stream().map(
                instance -> toExternalResource(account, instance)
        ).toList();
    }

    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunInstance> instance = describeInstance(account, externalResource.externalId());

        if(instance.isEmpty())
            return List.of();

        var tags = instance.get().detail().getTags();

        if(tags == null || Utils.isEmpty(tags.getTag()))
            return List.of();

        return tags.getTag().stream().map(
                tag -> new Tag(
                        new TagEntry(tag.getTagKey(), tag.getTagKey()),
                        tag.getTagValue(),
                        tag.getTagValue(),
                        0
                )
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunInstance instance = describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Instance not found: " + resource.getName())
        );


        List<AliyunDisk> disks = describeInstanceDisks(account, resource.getExternalId());

        Optional<AliyunDisk> systemDisk = disks.stream().filter(AliyunDisk::isSystemDisk).findAny();

        List<AliyunDisk> dataDisks = disks.stream().filter(
                disk -> !disk.isSystemDisk()
        ).toList();


        resource.updateByExternal(toExternalResource(account, instance));

        String flavor = "%sC%sG".formatted(instance.detail().getCpu(), instance.detail().getMemory()>>10);
        RuntimeProperty flavorProperty = RuntimeProperty.ofDisplayInList(
                "flavor", "规格大小", flavor, flavor
        );
        resource.addOrUpdateRuntimeProperty(flavorProperty);

        if(systemDisk.isPresent()){
            String systemDiskSize = systemDisk.get().detail().getSize().toString();
            RuntimeProperty systemDiskSizeProperty = RuntimeProperty.ofDisplayable(
                    "systemDiskSize", "系统盘大小(GB)", systemDiskSize, systemDiskSize
            );
            resource.addOrUpdateRuntimeProperty(systemDiskSizeProperty);
        }


        if(Utils.isNotEmpty(dataDisks)){
            String dataDisksSize = String.valueOf(getTotalDataDisksSize(dataDisks));
            RuntimeProperty dataDisksSizeProperty = RuntimeProperty.ofDisplayable(
                    "dataDisksSize", "数据盘总大小(GB)", dataDisksSize, dataDisksSize
            );
            resource.addOrUpdateRuntimeProperty(dataDisksSizeProperty);
        }

        List<String> publicIps = new ArrayList<>();
        List<String> privateIps = new ArrayList<>();
        List<String> ipv6Ips = new ArrayList<>();

        var networkInterfaces = instance.detail().getNetworkInterfaces();
        if(networkInterfaces != null && Utils.isNotEmpty(networkInterfaces.getNetworkInterface())){


            for (var networkInterface : networkInterfaces.getNetworkInterface()) {
                var privateIpSets = networkInterface.getPrivateIpSets();
                if(privateIpSets !=null && Utils.isNotEmpty(privateIpSets.getPrivateIpSet())){
                    for (var privateIpSet : privateIpSets.getPrivateIpSet()) {
                        privateIps.add(privateIpSet.getPrivateIpAddress());
                    }
                }

                var ipv6Sets = networkInterface.getIpv6Sets();
                if(ipv6Sets != null && Utils.isNotEmpty(ipv6Sets.getIpv6Set())){
                    for (var ipv6Set : ipv6Sets.getIpv6Set()) {
                        ipv6Ips.add(ipv6Set.getIpv6Address());
                    }
                }
            }


            RuntimeProperty privateIpsProperty = RuntimeProperty.ofDisplayInList(
                    "privateIps",
                    "内网IP",
                    String.join(",", privateIps),
                    String.join(",", privateIps)
            );
            resource.addOrUpdateRuntimeProperty(privateIpsProperty);


            RuntimeProperty ipv6IpsProperty = RuntimeProperty.ofDisplayable(
                    "ipv6Ips",
                    "IPv6地址",
                    String.join(",", ipv6Ips),
                    String.join(",", ipv6Ips)
            );
            resource.addOrUpdateRuntimeProperty(ipv6IpsProperty);

            if(!privateIps.isEmpty())
                RuntimePropertiesUtil.setManagementIp(resource, privateIps.get(0));
        }


        var publicIpAddress = instance.detail().getPublicIpAddress();
        if(publicIpAddress != null && Utils.isNotEmpty(publicIpAddress.getIpAddress())){
            publicIps.addAll(publicIpAddress.getIpAddress());
            String publicIpsText = String.join(",", publicIps);
            RuntimeProperty publicIpsProperty = RuntimeProperty.ofDisplayInList(
                    "publicIps", "公网IP", publicIpsText, publicIpsText
            );
            resource.addOrUpdateRuntimeProperty(publicIpsProperty);
        }



        if(instance.detail().getGPUAmount() != null && instance.detail().getGPUAmount() > 0){
            String gpuSpec = instance.detail().getGPUSpec();
            RuntimeProperty gpuSpecProperty = RuntimeProperty.ofDisplayable(
                    "gpuSpec", "GPU型号", gpuSpec, gpuSpec
            );

            String gpuAmount = instance.detail().getGPUAmount().toString();
            RuntimeProperty gpuAmountProperty = RuntimeProperty.ofDisplayable(
                    "gpuAmount", "GPU数量", gpuAmount, gpuAmount
            );

            resource.addOrUpdateRuntimeProperty(gpuSpecProperty);
            resource.addOrUpdateRuntimeProperty(gpuAmountProperty);
        }

        RuntimeProperty hostNameProperty = RuntimeProperty.ofDisplayable(
                "hostName", "主机名", instance.detail().getHostName(), instance.detail().getHostName()
        );

        resource.addOrUpdateRuntimeProperty(hostNameProperty);


        resource.updateUsageByType(UsageTypes.CPU_CORES, BigDecimal.valueOf(instance.detail().getCpu()));
        resource.updateUsageByType(UsageTypes.MEMORY_GB, BigDecimal.valueOf(instance.detail().getMemory()>>10));

        RuntimePropertiesUtil.autoSyncGuestManagementInfoQuietly(resource);

        if(Utils.isNotEmpty(publicIps))
            RuntimePropertiesUtil.setManagementIp(resource, publicIps.get(0));
        else if(Utils.isNotEmpty(privateIps))
            RuntimePropertiesUtil.setManagementIp(resource, privateIps.get(0));
    }

    private long getTotalDataDisksSize(List<AliyunDisk> dataDisks) {
        long sum = 0L;
        if(Utils.isEmpty(dataDisks))
            return sum;
        for (AliyunDisk dataDisk : dataDisks) {
            sum = sum + dataDisk.detail().getSize();
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

        AliyunInstance instance = describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Instance not found.")
        );

        AliyunClient client = provider.buildClient(account);

        switch (instance.detail().getInstanceChargeType()){
            case "PrePaid" -> {
                LocalDateTime createdTime = AliyunTimeUtil.toLocalDateMinutesTime(instance.detail().getCreationTime());
                LocalDateTime expiredTime = AliyunTimeUtil.toLocalDateMinutesTime(instance.detail().getExpiredTime());

                int months = (int) ChronoUnit.MONTHS.between(createdTime, expiredTime);


                var request = new DescribeRenewalPriceRequest();
                request.setResourceId(instance.detail().getInstanceId());
                request.setPeriod(months);

                Float tradePrice
                        = client.ecs().describeRenewalPrice(request).getPriceInfo().getPrice().getTradePrice();
                return new ResourceCost(tradePrice, months, ChronoUnit.MONTHS);
            }
            case "PostPaid" -> {
                var request = new DescribePriceRequest();
                request.setZoneId(instance.detail().getZoneId());
                request.setInstanceType(instance.detail().getInstanceType());
                request.setInternetMaxBandwidthOut(instance.detail().getInternetMaxBandwidthOut());
                request.setPriceUnit("Hour");

                List<AliyunDisk> disks = describeInstanceDisks(account, instance.detail().getInstanceId());


                List<DescribePriceRequest.DescribePriceRequestDataDisk> dataDisks = new ArrayList<>();
                for (AliyunDisk disk : disks) {
                    if(disk.isSystemDisk()){
                        var systemDisk = new DescribePriceRequest.DescribePriceRequestSystemDisk();
                        systemDisk.setSize(disk.detail().getSize());
                        systemDisk.setCategory(disk.detail().getCategory());
                        systemDisk.setPerformanceLevel(disk.detail().getPerformanceLevel());
                        request.setSystemDisk(systemDisk);
                    }else {
                        var dataDisk = new DescribePriceRequest.DescribePriceRequestDataDisk();
                        dataDisk.setSize(Long.valueOf(disk.detail().getSize()));
                        dataDisk.setCategory(disk.detail().getCategory());
                        dataDisk.setPerformanceLevel(disk.detail().getPerformanceLevel());
                        dataDisks.add(dataDisk);
                    }
                }

                if(Utils.isNotEmpty(dataDisks))
                    request.setDataDisk(dataDisks);

                var priceInfo = client.ecs().describePrice(request).getPriceInfo();
                Float tradePrice = priceInfo.getPrice().getTradePrice();
                return new ResourceCost(tradePrice, 1.0, ChronoUnit.HOURS);
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
        Optional<AliyunInstance> instance = describeInstance(account, resource.getExternalId());
        return instance.map(
                aliyunInstance -> aliyunInstance.isWindows() ? OsType.Windows : OsType.Linux
        ).orElse(OsType.Unknown);
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
        AliyunClient client = provider.buildClient(account);

        List<String> eventNames = AliyunEventTypes.instanceEventTypes.stream().map(
                AliyunEventTypes.AliyunEventType::externalEventName
        ).toList();

        List<AliyunEvent> events = client.trail().describeEvents(
                eventNames, externalId, happenedAfter
        );

        List<ExternalResourceEvent> result = new ArrayList<>();

        for (AliyunEvent event : events) {
            var aliyunEventType = AliyunEventTypes.fromInstanceEventName(event.getEventName());

            if(aliyunEventType.isEmpty())
                continue;

            ExternalResourceEvent externalResourceEvent = new ExternalResourceEvent(
                    event.getRequestId(),
                    aliyunEventType.get().eventType(),
                    StratoEventLevel.INFO,
                    StratoEventSource.EXTERNAL_ACTION,
                    getResourceTypeId(),
                    account.getId(),
                    externalId,
                    event.getEventName(),
                    AliyunTimeUtil.toLocalDateSecondsTime(event.getEventTime())
            );
            result.add(
                    externalResourceEvent
            );
        }

        return result;
    }
}
