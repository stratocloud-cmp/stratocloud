package com.stratocloud.provider.tencent.database.pg.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.database.pg.util.PgInstanceClass;
import com.stratocloud.provider.tencent.database.pg.TencentPgClassHandler;
import com.stratocloud.provider.tencent.zone.TencentZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentPgClassToZoneHandler implements RelationshipHandler {

    private final TencentPgClassHandler pgClassHandler;

    private final TencentZoneHandler zoneHandler;

    public TencentPgClassToZoneHandler(TencentPgClassHandler pgClassHandler,
                                       TencentZoneHandler zoneHandler) {
        this.pgClassHandler = pgClassHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_PG_CLASS_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云PostgreSQL规格与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return pgClassHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }

    @Override
    public String getCapabilityName() {
        return "支持的PostgreSQL规格";
    }

    @Override
    public String getRequirementName() {
        return "支持的可用区";
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
    public boolean disconnectOnLost() {
        return true;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<PgInstanceClass> instanceClass = pgClassHandler.describePgClass(account, source.externalId());

        if(instanceClass.isEmpty())
            return List.of();

        Set<String> zones = instanceClass.get().supportedZones();

        if(Utils.isEmpty(zones))
            return List.of();

        Set<String> zoneSet = new HashSet<>(zones);

        List<ExternalResource> zoneResources = zoneHandler.describeExternalResources(account);

        List<ExternalRequirement> result = new ArrayList<>();

        for (ExternalResource zoneResource : zoneResources) {
            if(zoneSet.contains(zoneResource.externalId()))
                result.add(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                zoneResource,
                                Map.of()
                        )
                );
        }

        return result;
    }
}
