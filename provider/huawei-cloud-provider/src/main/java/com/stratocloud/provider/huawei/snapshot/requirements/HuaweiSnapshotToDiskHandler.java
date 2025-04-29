package com.stratocloud.provider.huawei.snapshot.requirements;

import com.huaweicloud.sdk.evs.v2.model.SnapshotList;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.disk.HuaweiDiskHandler;
import com.stratocloud.provider.huawei.snapshot.HuaweiSnapshotHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiSnapshotToDiskHandler implements EssentialRequirementHandler {

    private final HuaweiSnapshotHandler snapshotHandler;

    private final HuaweiDiskHandler diskHandler;

    public HuaweiSnapshotToDiskHandler(HuaweiSnapshotHandler snapshotHandler,
                                       HuaweiDiskHandler diskHandler) {
        this.snapshotHandler = snapshotHandler;
        this.diskHandler = diskHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_SNAPSHOT_TO_DISK_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云硬盘与快照";
    }

    @Override
    public ResourceHandler getSource() {
        return snapshotHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return diskHandler;
    }

    @Override
    public String getCapabilityName() {
        return "快照";
    }

    @Override
    public String getRequirementName() {
        return "云硬盘";
    }

    @Override
    public String getConnectActionName() {
        return "添加";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<SnapshotList> snapshot = snapshotHandler.describeSnapshot(account, source.externalId());

        if(snapshot.isEmpty())
            return List.of();

        Optional<ExternalResource> disk = diskHandler.describeExternalResource(account, snapshot.get().getVolumeId());
        return disk.map(d -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        d,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
