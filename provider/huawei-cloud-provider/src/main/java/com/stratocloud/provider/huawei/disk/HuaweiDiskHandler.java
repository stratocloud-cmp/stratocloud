package com.stratocloud.provider.huawei.disk;

import com.huaweicloud.sdk.evs.v2.model.ListVolumesRequest;
import com.huaweicloud.sdk.evs.v2.model.VolumeDetail;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class HuaweiDiskHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiDiskHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_DISK";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云硬盘";
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

    public Optional<VolumeDetail> describeDisk(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).evs().describeVolume(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, VolumeDetail volume) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                volume.getId(),
                volume.getName(),
                convertStatus(volume.getStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        return switch (status) {
            case "available" -> ResourceState.IDLE;
            case "creating" -> ResourceState.BUILDING;
            case "in-use" -> ResourceState.IN_USE;
            case "error", "error_deleting" -> ResourceState.ERROR;
            case "attaching" -> ResourceState.ATTACHING;
            case "detaching" -> ResourceState.DETACHING;
            case "deleting" -> ResourceState.DESTROYING;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        HuaweiCloudClient client = provider.buildClient(account);
        return client.evs().describeVolumes(new ListVolumesRequest()).stream().map(
                disk -> toExternalResource(account, disk)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        VolumeDetail disk = describeDisk(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found.")
        );

        resource.updateByExternal(toExternalResource(account, disk));

        String sizeInfo = String.valueOf(disk.getSize());
        RuntimeProperty sizeProperty = RuntimeProperty.ofDisplayInList(
                "size", "磁盘大小(GB)", sizeInfo, sizeInfo
        );

        resource.addOrUpdateRuntimeProperty(sizeProperty);

        RuntimeProperty bootableProperty = RuntimeProperty.ofDisplayInList(
                "bootable",
                "是否为系统盘",
                disk.getBootable(),
                Objects.equals(disk.getBootable(), "true") ?"是":"否"
        );
        resource.addOrUpdateRuntimeProperty(bootableProperty);

        resource.updateUsageByType(UsageTypes.DISK_GB, BigDecimal.valueOf(disk.getSize()));
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of(
                UsageTypes.DISK_GB
        );
    }

    @Override
    public ResourceCost getCurrentCost(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        Optional<VolumeDetail> volumeDetail = describeDisk(account, resource.getExternalId());

        return volumeDetail.map(volume -> HuaweiDiskHelper.getPostPaidDiskCost(
                client,
                UUID.randomUUID().toString(),
                volume.getVolumeType(),
                volume.getSize(),
                volume.getAvailabilityZone()
        )).orElse(ResourceCost.ZERO);
    }
}
