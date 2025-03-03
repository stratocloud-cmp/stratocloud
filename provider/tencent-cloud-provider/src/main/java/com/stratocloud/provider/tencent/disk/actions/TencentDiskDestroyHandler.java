package com.stratocloud.provider.tencent.disk.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.disk.TencentDiskHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentDiskDestroyHandler implements DestroyResourceActionHandler {
    private final TencentDiskHandler diskHandler;

    public TencentDiskDestroyHandler(TencentDiskHandler diskHandler) {
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

        Optional<Disk> disk = diskHandler.describeDisk(account, resource.getExternalId());

        if(disk.isEmpty())
            return;

        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            return;

        TencentCloudProvider provider = (TencentCloudProvider) diskHandler.getProvider();
        provider.buildClient(account).deleteDisk(resource.getExternalId());
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
