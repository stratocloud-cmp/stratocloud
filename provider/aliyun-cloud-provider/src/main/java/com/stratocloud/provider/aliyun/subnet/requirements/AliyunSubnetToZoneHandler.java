package com.stratocloud.provider.aliyun.subnet.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnet;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnetHandler;
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
public class AliyunSubnetToZoneHandler implements EssentialRequirementHandler {

    private final AliyunSubnetHandler subnetHandler;

    private final AliyunZoneHandler zoneHandler;

    public AliyunSubnetToZoneHandler(AliyunSubnetHandler subnetHandler,
                                     AliyunZoneHandler zoneHandler) {
        this.subnetHandler = subnetHandler;
        this.zoneHandler = zoneHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_SUBNET_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "子网与可用区";
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
        Optional<AliyunSubnet> subnet = subnetHandler.describeSubnet(account, source.externalId());

        if(subnet.isEmpty())
            return List.of();

        Optional<ExternalResource> zone = zoneHandler.describeExternalResource(
                account, subnet.get().detail().getZoneId()
        );

        if(zone.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                zone.get(),
                Map.of()
        ));
    }
}
