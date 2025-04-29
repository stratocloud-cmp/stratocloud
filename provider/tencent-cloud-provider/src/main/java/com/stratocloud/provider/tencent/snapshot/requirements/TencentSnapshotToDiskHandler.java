package com.stratocloud.provider.tencent.snapshot.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.disk.TencentDiskHandler;
import com.stratocloud.provider.tencent.snapshot.TencentSnapshotHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.cbs.v20170312.models.Snapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentSnapshotToDiskHandler implements EssentialRequirementHandler {

    private final TencentSnapshotHandler snapshotHandler;

    private final TencentDiskHandler diskHandler;

    public TencentSnapshotToDiskHandler(TencentSnapshotHandler snapshotHandler,
                                        TencentDiskHandler diskHandler) {
        this.snapshotHandler = snapshotHandler;
        this.diskHandler = diskHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_SNAPSHOT_TO_DISK_RELATIONSHIP";
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
        Optional<Snapshot> snapshot = snapshotHandler.describeSnapshot(account, source.externalId());

        if(snapshot.isEmpty())
            return List.of();

        Optional<ExternalResource> disk = diskHandler.describeExternalResource(account, snapshot.get().getDiskId());
        return disk.map(d -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        d,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
