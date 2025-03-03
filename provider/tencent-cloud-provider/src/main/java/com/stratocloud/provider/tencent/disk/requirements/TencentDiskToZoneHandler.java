package com.stratocloud.provider.tencent.disk.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.disk.TencentDiskHandler;
import com.stratocloud.provider.tencent.zone.TencentZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentDiskToZoneHandler implements EssentialRequirementHandler {

    private final TencentDiskHandler diskHandler;

    private final TencentZoneHandler zoneHandler;

    public TencentDiskToZoneHandler(TencentDiskHandler diskHandler, TencentZoneHandler zoneHandler) {
        this.diskHandler = diskHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_DISK_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云硬盘与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return diskHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "云硬盘";
    }

    @Override
    public String getRequirementName() {
        return "可用区";
    }

    @Override
    public String getConnectActionName() {
        return "关联";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<Disk> disk = diskHandler.describeDisk(account, source.externalId());

        if(disk.isEmpty())
            return List.of();

        Optional<ExternalResource> zone
                = zoneHandler.describeExternalResource(account, disk.get().getPlacement().getZone());

        if(zone.isEmpty())
            return List.of();

        ExternalRequirement zoneRequirement = new ExternalRequirement(
                getRelationshipTypeId(),
                zone.get(),
                Map.of()
        );

        return List.of(zoneRequirement);
    }
}
