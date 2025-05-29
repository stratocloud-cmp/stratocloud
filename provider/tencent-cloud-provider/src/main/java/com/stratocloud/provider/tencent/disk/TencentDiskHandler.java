package com.stratocloud.provider.tencent.disk;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentTimeUtil;
import com.stratocloud.provider.tencent.disk.actions.TencentDiskType;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cbs.v20170312.models.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentDiskHandler extends AbstractResourceHandler {
    private final TencentCloudProvider provider;

    public TencentDiskHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_DISK";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云云硬盘";
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

    private ExternalResource toExternalResource(ExternalAccount account, Disk disk) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                disk.getDiskId(),
                disk.getDiskName(),
                convertState(disk.getDiskState())
        );
    }

    private ResourceState convertState(String diskState) {
        return switch (diskState){
            case "UNATTACHED" -> ResourceState.IDLE;
            case "ATTACHING" -> ResourceState.ATTACHING;
            case "ATTACHED" -> ResourceState.IN_USE;
            case "DETACHING" -> ResourceState.DETACHING;
            case "EXPANDING", "ROLLBACKING", "DUMPING" -> ResourceState.CONFIGURING;
            case "TORECYCLE" -> ResourceState.SHUTDOWN;
            default -> ResourceState.UNKNOWN;
        };
    }

    public Optional<Disk> describeDisk(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeDisk(externalId);
    }


    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        DescribeDisksRequest request = new DescribeDisksRequest();
        List<Disk> disks = provider.buildClient(account).describeDisks(request);
        return disks.stream().map(disk -> toExternalResource(account, disk)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<Disk> disk = describeDisk(account, externalResource.externalId());

        if(disk.isEmpty())
            return List.of();

        if(disk.get().getTags() == null)
            return List.of();

        return Arrays.stream(disk.get().getTags()).map(
                tag -> new Tag(new TagEntry(tag.getKey(), tag.getKey()), tag.getValue(), tag.getValue(), 0)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Disk disk = describeDisk(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found: " + resource.getExternalId())
        );

        ExternalResource externalResource = toExternalResource(account, disk);

        resource.updateByExternal(externalResource);

        String diskSize = disk.getDiskSize() != null ? disk.getDiskSize().toString() : "";
        RuntimeProperty sizeProperty = RuntimeProperty.ofDisplayInList(
                "size", "云硬盘大小(GB)", diskSize, diskSize
        );
        resource.addOrUpdateRuntimeProperty(sizeProperty);

        if(Utils.isNotBlank(disk.getDiskType())){
            Optional<TencentDiskType> tencentDiskType = TencentDiskType.fromId(disk.getDiskType());

            RuntimeProperty typeProperty = tencentDiskType.map(diskType -> RuntimeProperty.ofDisplayInList(
                    "type", "云硬盘类型", diskType.getId(), diskType.getName()
            )).orElseGet(() -> RuntimeProperty.ofDisplayInList(
                    "type", "云硬盘类型", disk.getDiskType(), disk.getDiskType()
            ));

            resource.addOrUpdateRuntimeProperty(typeProperty);
        }

        resource.updateUsageByType(UsageTypes.DISK_GB, BigDecimal.valueOf(disk.getDiskSize()));
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
        TencentCloudClient client = provider.buildClient(account);

        Disk disk = client.describeDisk(resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found.")
        );

        if(!disk.getPortable()){
            return ResourceCost.ZERO;
        }

        switch (disk.getDiskChargeType()){
            case "PREPAID" -> {
                LocalDateTime createdTime = TencentTimeUtil.toLocalDateTime(disk.getCreateTime());
                LocalDateTime expiredTime = TencentTimeUtil.toLocalDateTime(disk.getDeadlineTime());

                long months = ChronoUnit.MONTHS.between(createdTime, expiredTime);

                var request = new InquiryPriceRenewDisksRequest();
                request.setDiskIds(new String[]{disk.getDiskId()});
                DiskChargePrepaid prepaid = new DiskChargePrepaid();
                prepaid.setPeriod(months);
                request.setDiskChargePrepaids(new DiskChargePrepaid[]{prepaid});

                PrepayPrice diskPrice = client.inquiryPriceRenewDisk(request);
                return new ResourceCost(diskPrice.getDiscountPrice(), months, ChronoUnit.MONTHS);
            }
            case "POSTPAID_BY_HOUR" -> {
                var request = new InquiryPriceCreateDisksRequest();

                request.setDiskChargeType(disk.getDiskChargeType());
                request.setDiskType(disk.getDiskType());
                request.setDiskSize(disk.getDiskSize());
                request.setThroughputPerformance(disk.getThroughputPerformance());
                request.setDiskBackupQuota(disk.getDiskBackupQuota());

                Price diskPrice = client.inquiryPriceCreateDisk(request);

                return new ResourceCost(diskPrice.getUnitPriceDiscount(), 1.0, ChronoUnit.HOURS);
            }
            default -> {
                return ResourceCost.ZERO;
            }
        }
    }
}
