package com.stratocloud.provider.huawei.rds.requirements;

import com.huaweicloud.sdk.rds.v3.model.InstanceResponse;
import com.huaweicloud.sdk.rds.v3.model.NodeResponse;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
import com.stratocloud.provider.huawei.zone.HuaweiZoneHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class HuaweiRdsToZoneHandler implements EssentialRequirementHandler {

    private final HuaweiRdsHandler rdsHandler;

    private final HuaweiZoneHandler zoneHandler;

    public HuaweiRdsToZoneHandler(HuaweiRdsHandler rdsHandler, HuaweiZoneHandler zoneHandler) {
        this.rdsHandler = rdsHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_RDS_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云可用区与RDS实例";
    }

    @Override
    public ResourceHandler getSource() {
        return rdsHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "RDS实例";
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
        Optional<InstanceResponse> instance = rdsHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        List<NodeResponse> nodes = instance.get().getNodes();

        if(Utils.isEmpty(nodes))
            return List.of();

        Optional<NodeResponse> masterNode = nodes.stream().filter(
                n -> Objects.equals(n.getRole(), "master")
        ).findAny();

        if(masterNode.isEmpty())
            return List.of();

        String zoneId = masterNode.get().getAvailabilityZone();

        Optional<ExternalResource> zone = zoneHandler.describeExternalResource(account, zoneId);

        return zone.map(z -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        z,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
