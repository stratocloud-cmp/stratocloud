package com.stratocloud.provider.aliyun.instance;

import com.aliyun.ecs20140526.models.DescribeAvailableResourceRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ProviderStockException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.disk.AliyunDiskCategory;
import com.stratocloud.provider.aliyun.disk.actions.AliyunDiskBuildInput;
import com.stratocloud.provider.aliyun.flavor.AliyunFlavorId;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class AliyunInstanceUtil {
    public static void validateDiskCategoryMatchInstanceType(Resource disk, Resource instance) {
        var diskBuildInput = JSON.convert(disk.getProperties(), AliyunDiskBuildInput.class);

        boolean isSystemDisk = disk.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE);
        AliyunDiskCategory diskCategory = diskBuildInput.getDiskCategory();

        Optional<Resource> flavor = instance.getEssentialTarget(ResourceCategories.FLAVOR);

        if(flavor.isEmpty())
            return;

        AliyunFlavorId flavorId = AliyunFlavorId.fromString(flavor.get().getExternalId());
        String instanceType = flavorId.instanceTypeId();
        String zone = flavorId.zoneId();

        DescribeAvailableResourceRequest request = new DescribeAvailableResourceRequest();

        request.setZoneId(zone);
        request.setInstanceType(instanceType);
        request.setResourceType("instance");

        if(isSystemDisk){
            request.setDestinationResource("SystemDisk");
            request.setSystemDiskCategory(diskCategory.getId());
        }else {
            request.setDestinationResource("DataDisk");
            // System disk category is mandatory when destination resource is data disk and resource type is instance
            request.setSystemDiskCategory(diskCategory.getId());
            request.setDataDiskCategory(diskCategory.getId());
        }

        ExternalAccount account = disk.getResourceHandler().getAccountRepository().findExternalAccount(disk.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) disk.getResourceHandler().getProvider();

        AliyunClient client = provider.buildClient(account);

        var availableZones = client.ecs().describeAvailableResources(request);

        if(Utils.isNotEmpty(availableZones))
            for (var availableZone : availableZones)
                if(Utils.isNotEmpty(availableZone.getAvailableResources().getAvailableResource()))
                    return;


        log.error("Aliyun instance type {} is not compatible for disk category {}.",
                instanceType, diskCategory.getId());

        throw new ProviderStockException("云硬盘类型与实例规格不匹配");
    }
}
