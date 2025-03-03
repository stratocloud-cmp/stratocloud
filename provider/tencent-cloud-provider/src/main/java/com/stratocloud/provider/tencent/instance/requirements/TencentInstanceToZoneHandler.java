package com.stratocloud.provider.tencent.instance.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.provider.tencent.zone.TencentZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentInstanceToZoneHandler implements EssentialRequirementHandler {

    private final TencentInstanceHandler instanceHandler;

    private final TencentZoneHandler zoneHandler;

    public TencentInstanceToZoneHandler(TencentInstanceHandler instanceHandler, TencentZoneHandler zoneHandler) {
        this.instanceHandler = instanceHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_INSTANCE_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return instanceHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "云主机";
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
        Optional<Instance> instance = instanceHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        Optional<ExternalResource> zone
                = zoneHandler.describeExternalResource(account, instance.get().getPlacement().getZone());

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
