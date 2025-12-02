package com.stratocloud.provider.huawei.rocket.requirements;

import com.huaweicloud.sdk.rocketmq.v2.model.InstanceDetail;
import com.huaweicloud.sdk.rocketmq.v2.model.ListAvailableZonesRespAvailableZones;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.rocket.HuaweiRocketMqHandler;
import com.stratocloud.provider.huawei.zone.HuaweiZoneHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiRocketMqToZoneHandler implements EssentialRequirementHandler {

    private final HuaweiRocketMqHandler rocketMqHandler;

    private final HuaweiZoneHandler zoneHandler;

    public HuaweiRocketMqToZoneHandler(HuaweiRocketMqHandler rocketMqHandler, HuaweiZoneHandler zoneHandler) {
        this.rocketMqHandler = rocketMqHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_ROCKETMQ_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云可用区与RocketMQ实例";
    }

    @Override
    public ResourceHandler getSource() {
        return rocketMqHandler;
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
    public boolean visibleInTarget() {
        return false;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<InstanceDetail> instance = rocketMqHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        List<String> azIds = instance.get().getAvailableZones();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) rocketMqHandler.getProvider();
        var zones = provider.buildClient(account).rocket().describeZones();

        List<String> azCodes = zones.stream().filter(
                z -> azIds.contains(z.getId())
        ).map(ListAvailableZonesRespAvailableZones::getCode).toList();

        if(Utils.isEmpty(azCodes))
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();

        for (String azCode : azCodes) {
            zoneHandler.describeExternalResource(account, azCode).ifPresent(
                    er -> result.add(
                            new ExternalRequirement(
                                    getRelationshipTypeId(),
                                    er,
                                    Map.of()
                            )
                    )
            );
        }

        return result;
    }
}
