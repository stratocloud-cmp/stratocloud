package com.stratocloud.provider.tencent.database.pg.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.provider.tencent.subnet.TencentSubnetHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentPgToSubnetHandler implements EssentialRequirementHandler {

    private final TencentPgHandler pgHandler;

    private final TencentSubnetHandler subnetHandler;

    public TencentPgToSubnetHandler(TencentPgHandler pgHandler,
                                    TencentSubnetHandler subnetHandler) {
        this.pgHandler = pgHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_PG_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云PostgreSQL实例与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return pgHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return subnetHandler;
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
        return "子网";
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

        String subnetId = pg.get().getSubnetId();

        if(Utils.isBlank(subnetId))
            return List.of();

        Optional<ExternalResource> subnet = subnetHandler.describeExternalResource(account, subnetId);

        return subnet.map(
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
