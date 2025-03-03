package com.stratocloud.provider.huawei.servers.requirements;

import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
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
public class HuaweiServerToZoneHandler implements EssentialRequirementHandler {

    private final HuaweiServerHandler serverHandler;

    private final HuaweiZoneHandler zoneHandler;

    public HuaweiServerToZoneHandler(HuaweiServerHandler serverHandler,
                                     HuaweiZoneHandler zoneHandler) {
        this.serverHandler = serverHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_SERVER_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return serverHandler;
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
        Optional<ServerDetail> server = serverHandler.describeServer(account, source.externalId());

        if(server.isEmpty())
            return List.of();

        Optional<ExternalResource> zone
                = zoneHandler.describeExternalResource(account, server.get().getOsEXTAZAvailabilityZone());

        return zone.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
