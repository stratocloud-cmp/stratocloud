package com.stratocloud.provider.aliyun.instance.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.aliyun.zone.AliyunZoneHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunInstanceToZoneHandler implements EssentialRequirementHandler {

    private final AliyunInstanceHandler instanceHandler;

    private final AliyunZoneHandler zoneHandler;

    public AliyunInstanceToZoneHandler(AliyunInstanceHandler instanceHandler, AliyunZoneHandler zoneHandler) {
        this.instanceHandler = instanceHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_INSTANCE_TO_ZONE_RELATIONSHIP";
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
        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        Optional<ExternalResource> zone
                = zoneHandler.describeExternalResource(account, instance.get().detail().getZoneId());

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
