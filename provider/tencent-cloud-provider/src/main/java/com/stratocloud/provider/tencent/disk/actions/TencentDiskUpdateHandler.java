package com.stratocloud.provider.tencent.disk.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.disk.TencentDiskHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import com.tencentcloudapi.cbs.v20170312.models.ModifyDiskAttributesRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentDiskUpdateHandler implements ResourceActionHandler {

    private final TencentDiskHandler diskHandler;

    public TencentDiskUpdateHandler(TencentDiskHandler diskHandler) {
        this.diskHandler = diskHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return diskHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新云硬盘";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentDiskUpdateInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentDiskUpdateInput updateInput = JSON.convert(parameters, TencentDiskUpdateInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) diskHandler.getProvider();

        ModifyDiskAttributesRequest request = new ModifyDiskAttributesRequest();

        request.setDiskIds(new String[]{resource.getExternalId()});
        request.setDiskName(updateInput.getDiskName());

        if(updateInput.getDiskType() != null)
            request.setDiskType(updateInput.getDiskType().getId());

        if(updateInput.getBurstPerformance()!=null && updateInput.getBurstPerformance())
            request.setBurstPerformanceOperation("CREATE");

        provider.buildClient(account).modifyDisk(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<Disk> disk = diskHandler.describeDisk(account, resource.getExternalId());

        if(disk.isEmpty())
            return ResourceActionResult.failed("Disk not found.");


        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
