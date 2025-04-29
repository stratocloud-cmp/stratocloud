package com.stratocloud.provider.huawei.snapshot.actions;

import com.huaweicloud.sdk.evs.v2.model.CreateSnapshotOption;
import com.huaweicloud.sdk.evs.v2.model.CreateSnapshotRequest;
import com.huaweicloud.sdk.evs.v2.model.CreateSnapshotRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.snapshot.HuaweiSnapshotHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HuaweiSnapshotBuildHandler implements BuildResourceActionHandler {

    private final HuaweiSnapshotHandler snapshotHandler;

    public HuaweiSnapshotBuildHandler(HuaweiSnapshotHandler snapshotHandler) {
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
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource diskResource = resource.getEssentialTarget(ResourceCategories.DISK).orElseThrow(
                () -> new StratoException("Disk resource not found when creating snapshot")
        );

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) snapshotHandler.getProvider();

        CreateSnapshotRequest request = new CreateSnapshotRequest();
        request.withBody(
                new CreateSnapshotRequestBody().withSnapshot(
                        new CreateSnapshotOption()
                                .withVolumeId(diskResource.getExternalId())
                                .withName(resource.getName())
                                .withDescription(resource.getDescription())
                                .withForce(true)
                )
        );

        String snapshotId = provider.buildClient(account).evs().createSnapshot(request);

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
