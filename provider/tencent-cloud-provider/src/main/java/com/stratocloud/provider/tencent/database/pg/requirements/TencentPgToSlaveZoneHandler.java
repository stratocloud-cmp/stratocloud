package com.stratocloud.provider.tencent.database.pg.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.provider.tencent.zone.TencentZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.postgres.v20170312.models.DBNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentPgToSlaveZoneHandler implements ExclusiveRequirementHandler {

    private final TencentPgHandler pgHandler;

    private final TencentZoneHandler zoneHandler;

    public TencentPgToSlaveZoneHandler(TencentPgHandler pgHandler,
                                       TencentZoneHandler zoneHandler) {
        this.pgHandler = pgHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_PG_TO_SLAVE_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云PostgreSQL实例与备可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return pgHandler;
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
    public boolean visibleInForm() {
        return false;
    }

    @Override
    public String getCapabilityName() {
        return "PostgreSQL实例";
    }

    @Override
    public String getRequirementName() {
        return "备可用区";
    }

    @Override
    public String getConnectActionName() {
        return "设置";
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
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        var pg = pgHandler.describePg(account, source.externalId());

        if(pg.isEmpty())
            return List.of();

        DBNode[] dbNodeSet = pg.get().getDBNodeSet();

        if(Utils.isEmpty(dbNodeSet))
            return List.of();

        List<DBNode> standbyNodes = Arrays.stream(dbNodeSet).filter(
                n -> Objects.equals(n.getRole(), "Standby")
        ).toList();

        List<ExternalResource> zones = zoneHandler.describeExternalResources(account);

        return zones.stream().filter(
                z -> standbyNodes.stream().anyMatch(
                        n -> Objects.equals(z.externalId(), n.getZone())
                )
        ).map(
                z -> new ExternalRequirement(
                        getRelationshipTypeId(),
                        z,
                        Map.of()
                )
        ).toList();
    }
}
