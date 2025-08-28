package com.stratocloud.provider.tencent.database.pg.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.database.pg.util.PgInstanceClass;
import com.stratocloud.provider.tencent.database.pg.TencentPgClassHandler;
import com.stratocloud.provider.tencent.database.pg.TencentPgVersionHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.postgres.v20170312.models.Version;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentPgClassToVersionHandler implements RelationshipHandler {

    private final TencentPgClassHandler pgClassHandler;

    private final TencentPgVersionHandler pgVersionHandler;

    public TencentPgClassToVersionHandler(TencentPgClassHandler pgClassHandler,
                                          TencentPgVersionHandler pgVersionHandler) {
        this.pgClassHandler = pgClassHandler;
        this.pgVersionHandler = pgVersionHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_PG_CLASS_TO_VERSION_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云PostgreSQL规格与版本";
    }

    @Override
    public ResourceHandler getSource() {
        return pgClassHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return pgVersionHandler;
    }

    @Override
    public String getCapabilityName() {
        return "支持的规格";
    }

    @Override
    public String getRequirementName() {
        return "支持的版本";
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

        Set<Version> versions = instanceClass.get().supportedVersions();

        if(Utils.isEmpty(versions))
            return List.of();

        return versions.stream().map(
                v -> new ExternalRequirement(
                        getRelationshipTypeId(),
                        pgVersionHandler.toExternalResource(account, v),
                        Map.of()
                )
        ).toList();
    }
}
