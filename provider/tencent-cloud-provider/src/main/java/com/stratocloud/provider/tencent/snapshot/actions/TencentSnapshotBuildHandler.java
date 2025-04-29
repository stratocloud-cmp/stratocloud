package com.stratocloud.provider.tencent.snapshot.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentTimeUtil;
import com.stratocloud.provider.tencent.snapshot.TencentSnapshotHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.cbs.v20170312.models.CreateSnapshotRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class TencentSnapshotBuildHandler implements BuildResourceActionHandler {

    private final TencentSnapshotHandler snapshotHandler;

    public TencentSnapshotBuildHandler(TencentSnapshotHandler snapshotHandler) {
        this.snapshotHandler = snapshotHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return snapshotHandler;
    }

    @Override
    public String getTaskName() {
        return "创建快照";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentSnapshotBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource diskResource = resource.getEssentialTarget(ResourceCategories.DISK).orElseThrow(
                () -> new StratoException("Disk resource not found when creating snapshot")
        );
        TencentSnapshotBuildInput input = JSON.convert(parameters, TencentSnapshotBuildInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) snapshotHandler.getProvider();

        CreateSnapshotRequest request = new CreateSnapshotRequest();
        request.setSnapshotName(resource.getName());
        request.setDiskId(diskResource.getExternalId());

        if(input.getRetentionDays() != null)
            request.setDeadline(
                    TencentTimeUtil.fromLocalDateTime(LocalDateTime.now().plusDays(input.getRetentionDays()))
            );

        String snapshotId = provider.buildClient(account).createSnapshot(request);

        resource.setExternalId(snapshotId);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
