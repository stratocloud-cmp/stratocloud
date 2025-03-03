package com.stratocloud.provider.huawei.subnet.requirements;

import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.subnet.HuaweiSubnetHandler;
import com.stratocloud.provider.huawei.zone.HuaweiZoneHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiSubnetToZoneHandler implements EssentialRequirementHandler {

    private final HuaweiSubnetHandler subnetHandler;

    private final HuaweiZoneHandler zoneHandler;

    public HuaweiSubnetToZoneHandler(HuaweiSubnetHandler subnetHandler, HuaweiZoneHandler zoneHandler) {
        this.subnetHandler = subnetHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_SUBNET_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云可用区与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return subnetHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "子网";
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
        Optional<Subnet> subnet = subnetHandler.describeSubnet(account, source.externalId());

        if(subnet.isEmpty())
            return List.of();

        String zoneId = subnet.get().getAvailabilityZone();

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
