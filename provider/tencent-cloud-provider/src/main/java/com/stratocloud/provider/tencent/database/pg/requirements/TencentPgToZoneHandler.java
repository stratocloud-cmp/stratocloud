package com.stratocloud.provider.tencent.database.pg.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.provider.tencent.zone.TencentZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentPgToZoneHandler implements EssentialRequirementHandler {

    private final TencentPgHandler pgHandler;

    private final TencentZoneHandler zoneHandler;

    public TencentPgToZoneHandler(TencentPgHandler pgHandler,
                                  TencentZoneHandler zoneHandler) {
        this.pgHandler = pgHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_PG_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云PostgreSQL实例与主可用区";
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
    public String getCapabilityName() {
        return "PostgreSQL实例";
    }

    @Override
    public String getRequirementName() {
        return "主可用区";
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
        var pg = pgHandler.describePg(account, source.externalId());

        if(pg.isEmpty())
            return List.of();

        String zone = pg.get().getZone();

        if(Utils.isBlank(zone))
            return List.of();

        Optional<ExternalResource> zoneResource = zoneHandler.describeExternalResource(account, zone);

        return zoneResource.map(
                er -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                er,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
