package com.stratocloud.provider.huawei.elb.zone;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.zone.HuaweiZoneHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class HuaweiElbZoneToZoneSetHandler implements RelationshipHandler {

    private final HuaweiZoneHandler zoneHandler;

    private final HuaweiElbZoneSetHandler zoneSetHandler;

    public HuaweiElbZoneToZoneSetHandler(HuaweiZoneHandler zoneHandler,
                                         HuaweiElbZoneSetHandler zoneSetHandler) {
        this.zoneHandler = zoneHandler;
        this.zoneSetHandler = zoneSetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_ZONE_TO_ZONE_SET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "可用区与可用区集合";
    }

    @Override
    public ResourceHandler getSource() {
        return zoneHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneSetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "可用区";
    }

    @Override
    public String getRequirementName() {
        return "可用区集合";
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
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) zoneHandler.getProvider();

        List<HuaweiElbZoneSet> zoneSets = provider.buildClient(account).elb().describeZoneSets();

        return zoneSets.stream().filter(
                zoneSet -> zoneSet.containsZone(source.externalId())
        ).map(
                zoneSet -> zoneSetHandler.toExternalResource(account, zoneSet)
        ).map(
                er -> new ExternalRequirement(
                        getRelationshipTypeId(),
                        er,
                        Map.of()
                )
        ).toList();
    }
}
