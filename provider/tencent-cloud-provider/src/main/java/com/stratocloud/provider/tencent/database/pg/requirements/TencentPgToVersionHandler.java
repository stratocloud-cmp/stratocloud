package com.stratocloud.provider.tencent.database.pg.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.provider.tencent.database.pg.TencentPgVersionHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentPgToVersionHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "TENCENT_PG_TO_VERSION_RELATIONSHIP";
    private final TencentPgHandler pgHandler;

    private final TencentPgVersionHandler pgVersionHandler;

    public TencentPgToVersionHandler(TencentPgHandler pgHandler, TencentPgVersionHandler pgVersionHandler) {
        this.pgHandler = pgHandler;
        this.pgVersionHandler = pgVersionHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云PostgreSQL实例与版本";
    }

    @Override
    public ResourceHandler getSource() {
        return pgHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return pgVersionHandler;
    }

    @Override
    public String getCapabilityName() {
        return "实例";
    }

    @Override
    public String getRequirementName() {
        return "版本";
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

        String kernelVersion = pg.get().getDBKernelVersion();

        if(Utils.isBlank(kernelVersion))
            return List.of();

        var version = pgVersionHandler.describeExternalResource(account, kernelVersion);

        return version.map(
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
