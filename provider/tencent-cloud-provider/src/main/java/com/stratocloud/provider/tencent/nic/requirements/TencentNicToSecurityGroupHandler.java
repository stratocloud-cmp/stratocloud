package com.stratocloud.provider.tencent.nic.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.nic.TencentNicHandler;
import com.stratocloud.provider.tencent.securitygroup.TencentSecurityGroupHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.NetworkInterface;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentNicToSecurityGroupHandler implements RelationshipHandler {

    private final TencentNicHandler nicHandler;

    private final TencentSecurityGroupHandler securityGroupHandler;

    public TencentNicToSecurityGroupHandler(TencentNicHandler nicHandler,
                                            TencentSecurityGroupHandler securityGroupHandler) {
        this.nicHandler = nicHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_NIC_TO_SECURITY_GROUP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "网卡与安全组";
    }

    @Override
    public ResourceHandler getSource() {
        return nicHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return securityGroupHandler;
    }

    @Override
    public String getCapabilityName() {
        return "网卡";
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
        Resource nic = relationship.getSource();
        Resource securityGroup = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(nic.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) nicHandler.getProvider();

        TencentCloudClient client = provider.buildClient(account);

        client.associateNicToSecurityGroup(nic.getExternalId(), securityGroup.getExternalId());
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource nic = relationship.getSource();
        Resource securityGroup = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(nic.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) nicHandler.getProvider();

        Optional<SecurityGroup> optionalSg = securityGroupHandler.describeSecurityGroup(
                account, securityGroup.getExternalId()
        );

        if(optionalSg.isEmpty())
            return;

        if(optionalSg.get().getIsDefault())
            return;

        TencentCloudClient client = provider.buildClient(account);

        client.disassociateNicFromSecurityGroup(nic.getExternalId(), securityGroup.getExternalId());
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
        Optional<NetworkInterface> nic = nicHandler.describeNic(account, source.externalId());

        if(nic.isEmpty())
            return List.of();

        if(Utils.isEmpty(nic.get().getGroupSet()))
            return List.of();


        List<ExternalRequirement> result = new ArrayList<>();
        for (String groupId : nic.get().getGroupSet()) {
            Optional<ExternalResource> group = securityGroupHandler.describeExternalResource(
                    account, groupId
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
