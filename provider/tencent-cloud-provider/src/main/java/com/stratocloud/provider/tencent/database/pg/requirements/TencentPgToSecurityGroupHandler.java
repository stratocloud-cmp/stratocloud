package com.stratocloud.provider.tencent.database.pg.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.provider.tencent.securitygroup.TencentSecurityGroupHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.tencentcloudapi.postgres.v20170312.models.SecurityGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TencentPgToSecurityGroupHandler implements RelationshipHandler {

    private final TencentPgHandler pgHandler;

    private final TencentSecurityGroupHandler securityGroupHandler;

    public TencentPgToSecurityGroupHandler(TencentPgHandler pgHandler,
                                           TencentSecurityGroupHandler securityGroupHandler) {
        this.pgHandler = pgHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_PG_TO_SECURITY_GROUP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云PostgreSQL实例与安全组";
    }

    @Override
    public ResourceHandler getSource() {
        return pgHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return securityGroupHandler;
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
        return "安全组";
    }

    @Override
    public String getConnectActionName() {
        return "绑定";
    }

    @Override
    public String getDisconnectActionName() {
        return "解绑";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource pg = relationship.getSource();
        Resource securityGroup = relationship.getTarget();

        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(pg.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        client.associatePgSecurityGroup(pg.getExternalId(), securityGroup.getExternalId());
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource pg = relationship.getSource();
        Resource securityGroup = relationship.getTarget();

        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(pg.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        client.disassociatePgSecurityGroup(pg.getExternalId(), securityGroup.getExternalId());
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);
        List<SecurityGroup> securityGroups = client.describePgSecurityGroups(source.externalId());

        List<ExternalRequirement> result = new ArrayList<>();

        for (SecurityGroup securityGroup : securityGroups) {
            var sgResource = securityGroupHandler.describeExternalResource(account, securityGroup.getSecurityGroupId());

            sgResource.ifPresent(
                    r -> result.add(
                            new ExternalRequirement(
                                    getRelationshipTypeId(),
                                    r,
                                    Map.of()
                            )
                    )
            );
        }

        return result;
    }

    @Override
    public boolean requireTargetResourceTaskLock() {
        return true;
    }
}
