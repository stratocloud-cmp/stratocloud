package com.stratocloud.provider.aliyun.disk;

import com.aliyun.ecs20140526.models.*;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.AliyunTimeUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.monitor.MonitoredResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.resource.monitor.ResourceQuickStats;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunDiskHandler extends AbstractResourceHandler implements MonitoredResourceHandler {
    private final AliyunCloudProvider provider;

    public AliyunDiskHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_DISK";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云云硬盘";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.DISK;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeDisk(account, externalId).map(
                disk -> toExternalResource(account, disk)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunDisk disk) {
        String diskId = disk.detail().getDiskId();
        String diskName = disk.detail().getDiskName();

        String resourceName = Utils.isNotBlank(diskName) ? diskName :diskId;

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                diskId,
                resourceName,
                convertState(disk.detail().getStatus())
        );
    }

    private ResourceState convertState(String status) {
        return switch (status){
            case "Creating" -> ResourceState.BUILDING;
            case "Available" -> ResourceState.IDLE;
            case "Attaching" -> ResourceState.ATTACHING;
            case "In_use" -> ResourceState.IN_USE;
            case "Detaching" -> ResourceState.DETACHING;
            case "ReIniting" -> ResourceState.CONFIGURING;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<AliyunDisk> describeDisk(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeDisk(externalId);
    }


    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        DescribeDisksRequest request = new DescribeDisksRequest();
        List<AliyunDisk> disks = provider.buildClient(account).ecs().describeDisks(request);
        return disks.stream().map(disk -> toExternalResource(account, disk)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunDisk> disk = describeDisk(account, externalResource.externalId());

        if(disk.isEmpty())
            return List.of();

        var tags = disk.get().detail().getTags();
        if(tags == null || Utils.isEmpty(tags.getTag()))
            return List.of();

        return tags.getTag().stream().map(
                tag -> new Tag(new TagEntry(tag.getTagKey(), tag.getTagKey()), tag.getTagValue(), tag.getTagValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunDisk disk = describeDisk(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found: " + resource.getExternalId())
        );

        ExternalResource externalResource = toExternalResource(account, disk);

        resource.updateByExternal(externalResource);

        String diskSize = disk.detail().getSize() != null ? disk.detail().getSize().toString() : "";
        RuntimeProperty sizeProperty = RuntimeProperty.ofDisplayInList(
                "size", "云硬盘大小(GB)", diskSize, diskSize
        );
        resource.addOrUpdateRuntimeProperty(sizeProperty);

        RuntimeProperty typeProperty = RuntimeProperty.ofDisplayInList(
                "type", "云硬盘用途", disk.detail().getType(), disk.isSystemDisk()?"系统盘":"数据盘"
        );
        resource.addOrUpdateRuntimeProperty(typeProperty);

        String category = disk.detail().getCategory();
        if(Utils.isNotBlank(category)){
            Optional<AliyunDiskCategory> aliyunDiskCategory = AliyunDiskCategory.fromId(category);

            RuntimeProperty categoryProperty = aliyunDiskCategory.map(diskType -> RuntimeProperty.ofDisplayInList(
                    "category", "云硬盘类型", diskType.getId(), diskType.getName()
            )).orElseGet(() -> RuntimeProperty.ofDisplayInList(
                    "category", "云硬盘类型", category, category
            ));

            resource.addOrUpdateRuntimeProperty(categoryProperty);
        }

        String performanceLevel = disk.detail().getPerformanceLevel();
        if(Utils.isNotBlank(performanceLevel)){
            RuntimeProperty performanceLevelProperty =RuntimeProperty.ofDisplayInList(
                    "performanceLevel",
                    "性能等级",
                    disk.detail().getPerformanceLevel(),
                    disk.detail().getPerformanceLevel()
            );
            resource.addOrUpdateRuntimeProperty(performanceLevelProperty);
        }

        resource.updateUsageByType(UsageTypes.DISK_GB, BigDecimal.valueOf(disk.detail().getSize()));
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(UsageTypes.DISK_GB);
    }

    @Override
    public ResourceCost getCurrentCost(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return ResourceCost.ZERO;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        AliyunDisk disk = client.ecs().describeDisk(resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found.")
        );

        switch (disk.detail().getDiskChargeType()){
            case "PrePaid" -> {
                LocalDateTime createdTime = AliyunTimeUtil.toLocalDateTime(disk.detail().getCreationTime());
                LocalDateTime expiredTime = AliyunTimeUtil.toLocalDateTime(disk.detail().getExpiredTime());

                int months = (int) ChronoUnit.MONTHS.between(createdTime, expiredTime);

                var request = new DescribeRenewalPriceRequest();
                request.setResourceId(disk.detail().getDiskId());
                request.setResourceType("disk");
                request.setPeriod(months);
                request.setPriceUnit("Month");

                var priceInfo = client.ecs().describeRenewalPrice(request).getPriceInfo();
                return new ResourceCost(priceInfo.getPrice().getTradePrice(), months, ChronoUnit.MONTHS);
            }
            case "PostPaid" -> {
                var request = new DescribePriceRequest();

                request.setResourceType("disk");

                var dataDisk = new DescribePriceRequest.DescribePriceRequestDataDisk();

                dataDisk.setCategory(disk.detail().getCategory());
                dataDisk.setSize(Long.valueOf(disk.detail().getSize()));
                dataDisk.setPerformanceLevel(disk.detail().getPerformanceLevel());

                request.setDataDisk(List.of(dataDisk));

                var priceInfo = client.ecs().describePrice(request).getPriceInfo();

                return new ResourceCost(priceInfo.getPrice().getTradePrice(), 1.0, ChronoUnit.HOURS);
            }
            default -> {
                return ResourceCost.ZERO;
            }
        }
    }

    @Override
    public Optional<ResourceQuickStats> describeQuickStats(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunDisk> aliyunDisk = describeDisk(account, resource.getExternalId());

        if(aliyunDisk.isEmpty())
            return Optional.empty();

        var disk = aliyunDisk.get().detail();

        if(Utils.isBlank(disk.getInstanceId()))
            return Optional.empty();

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minus(Duration.ofSeconds(180L));

        DescribeDiskMonitorDataRequest request = new DescribeDiskMonitorDataRequest();
        request.setDiskId(disk.getDiskId());
        request.setStartTime(AliyunTimeUtil.toAliyunDateTime(startTime));
        request.setEndTime(AliyunTimeUtil.toAliyunDateTime(endTime));
        request.setPeriod(60);

        var responseBody = provider.buildClient(account).ecs().describeDiskMonitorData(request);

        if(Utils.isEmpty(responseBody.getMonitorData().getDiskMonitorData()))
            return Optional.empty();

        var dataPoint = responseBody.getMonitorData().getDiskMonitorData().get(0);

        Integer bpsRead = dataPoint.getBPSRead();
        Integer bpsWrite = dataPoint.getBPSWrite();

        ResourceQuickStats.Builder builder = ResourceQuickStats.builder();
        builder.addItem(
                "r", "读速率", bpsRead/(1024.0*1024.0), "MBps"
        );
        builder.addItem(
                "w", "写速率", bpsWrite/(1024.0*1024.0), "MBps"
        );

        return Optional.of(builder.build());
    }
}
