package com.stratocloud.provider.huawei.disk.actions;

import com.huaweicloud.sdk.evs.v2.model.VolumeDetail;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.disk.HuaweiDiskHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiDiskDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiDiskHandler diskHandler;

    public HuaweiDiskDestroyHandler(HuaweiDiskHandler diskHandler) {
        this.diskHandler = diskHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return diskHandler;
    }

    @Override
    public String getTaskName() {
        return "删除云硬盘";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<VolumeDetail> disk = diskHandler.describeDisk(account, resource.getExternalId());

        if(disk.isEmpty())
            return;

        if("true".equals(disk.get().getBootable()) && Utils.isNotEmpty(disk.get().getAttachments())){
            log.warn("Disk {} is a attached system disk, skipping DESTROY action...",
                    disk.get().getName());
            return;
        }

        HuaweiCloudProvider provider = (HuaweiCloudProvider) diskHandler.getProvider();
        provider.buildClient(account).evs().deleteVolume(disk.get().getId());
    }


    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE)) {
            resource.onDestroyed();
            return ResourceActionResult.finished();
        }

        return DestroyResourceActionHandler.super.checkActionResult(resource, parameters);
    }
}
