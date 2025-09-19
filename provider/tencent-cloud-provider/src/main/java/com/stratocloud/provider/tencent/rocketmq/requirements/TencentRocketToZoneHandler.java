package com.stratocloud.provider.tencent.rocketmq.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.rocketmq.TencentRocketHandler;
import com.stratocloud.provider.tencent.rocketmq.TencentRocketZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class TencentRocketToZoneHandler implements EssentialRequirementHandler {

    private final TencentRocketHandler rocketHandler;

    private final TencentRocketZoneHandler zoneHandler;

    public TencentRocketToZoneHandler(TencentRocketHandler rocketHandler,
                                      TencentRocketZoneHandler zoneHandler) {
        this.rocketHandler = rocketHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_ROCKETMQ_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云RocketMQ实例与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return rocketHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "RocketMQ实例";
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
    public int compareRequirement(RelationshipHandler other) {
        return -1;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        TencentCloudProvider provider = (TencentCloudProvider) rocketHandler.getProvider();
        var rocket = provider.buildClient(account).describeRocketInstanceDetail(source.externalId());

        if(rocket.isEmpty())
            return List.of();

        Long[] zoneIds = rocket.get().getZoneIds();

        if(Utils.isEmpty(zoneIds))
            return List.of();

        return zoneHandler.describeExternalResources(account, Map.of()).stream().filter(
                r -> Arrays.stream(zoneIds).map(String::valueOf).toList().contains(r.externalId())
        ).map(
                r -> new ExternalRequirement(
                        getRelationshipTypeId(),
                        r,
                        Map.of()
                )
        ).toList();
    }
}
