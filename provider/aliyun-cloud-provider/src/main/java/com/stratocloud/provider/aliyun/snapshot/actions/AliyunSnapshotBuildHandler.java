package com.stratocloud.provider.aliyun.snapshot.actions;

import com.aliyun.ecs20140526.models.CreateSnapshotRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.snapshot.AliyunSnapshotHandler;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AliyunSnapshotBuildHandler implements BuildResourceActionHandler {

    private final AliyunSnapshotHandler snapshotHandler;

    public AliyunSnapshotBuildHandler(AliyunSnapshotHandler snapshotHandler) {
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
        return AliyunSnapshotBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunSnapshotBuildInput input = JSON.convert(parameters, AliyunSnapshotBuildInput.class);

        Resource diskResource = resource.getEssentialTarget(ResourceCategories.DISK).orElseThrow(
                () -> new StratoException("Disk resource not found when creating snapshot")
        );

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) snapshotHandler.getProvider();

        CreateSnapshotRequest request = new CreateSnapshotRequest();
        request.setSnapshotName(resource.getName())
                .setDescription(resource.getDescription())
                .setDiskId(diskResource.getExternalId())
                .setRetentionDays(input.getRetentionDays());

        String snapshotId = provider.buildClient(account).ecs().createSnapshot(request);

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
