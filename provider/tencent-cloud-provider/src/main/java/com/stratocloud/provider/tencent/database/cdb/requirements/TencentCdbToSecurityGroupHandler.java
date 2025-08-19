package com.stratocloud.provider.tencent.database.cdb.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.provider.tencent.securitygroup.TencentSecurityGroupHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentCdbToSecurityGroupHandler implements RelationshipHandler {

    private final TencentCdbHandler cdbHandler;

    private final TencentSecurityGroupHandler securityGroupHandler;

    public TencentCdbToSecurityGroupHandler(TencentCdbHandler cdbHandler,
                                            TencentSecurityGroupHandler securityGroupHandler) {
        this.cdbHandler = cdbHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_CDB_TO_SECURITY_GROUP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云数据库与安全组";
    }

    @Override
    public ResourceHandler getSource() {
        return cdbHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return securityGroupHandler;
    }

    @Override
    public String getCapabilityName() {
        return "云数据库";
    }

    @Override
    public String getRequirementName() {
        return "安全组";
    }

    @Override
    public String getConnectActionName() {
        return "绑定安全组";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除绑定";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource cdb = relationship.getSource();
        Resource securityGroup = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(cdb.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();

        TencentCloudClient client = provider.buildClient(account);

        client.associateCdbToSecurityGroup(cdb.getExternalId(), securityGroup.getExternalId());
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource cdb = relationship.getSource();
        Resource securityGroup = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(cdb.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();

        Optional<SecurityGroup> optionalSg = securityGroupHandler.describeSecurityGroup(
                account, securityGroup.getExternalId()
        );

        if(optionalSg.isEmpty())
            return;

        if(optionalSg.get().getIsDefault())
            return;

        TencentCloudClient client = provider.buildClient(account);

        client.disassociateCdbFromSecurityGroup(cdb.getExternalId(), securityGroup.getExternalId());
    }

    @Override
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        Optional<SecurityGroup> securityGroup = securityGroupHandler.describeSecurityGroup(
                account, relationship.getTarget().getExternalId()
        );

        if(securityGroup.isPresent() && securityGroup.get().getIsDefault())
            return RelationshipActionResult.finished();

        return RelationshipHandler.super.checkDisconnectResult(account, relationship);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<ExternalResource> cdb = cdbHandler.describeExternalResource(account, source.externalId());

        if(cdb.isEmpty())
            return List.of();

        if(cdb.get().state() == ResourceState.SHUTDOWN)
            return List.of();

        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        var securityGroups = provider.buildClient(account).describeCdbSecurityGroups(cdb.get().externalId());

        if(Utils.isEmpty(securityGroups))
            return List.of();


        List<ExternalRequirement> result = new ArrayList<>();
        for (var sg : securityGroups) {
            Optional<ExternalResource> group = securityGroupHandler.describeExternalResource(
                    account, sg.getSecurityGroupId()
            );

            if(group.isEmpty())
                continue;

            result.add(new ExternalRequirement(
                    getRelationshipTypeId(),
                    group.get(),
                    Map.of()
            ));
        }

        return result;
    }
}
