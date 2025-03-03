package com.stratocloud.provider.aliyun.nic.requirements;

import com.aliyun.ecs20140526.models.JoinSecurityGroupRequest;
import com.aliyun.ecs20140526.models.LeaveSecurityGroupRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.nic.AliyunNicHandler;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroup;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroupHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AliyunNicToSecurityGroupHandler implements RelationshipHandler {

    private final AliyunNicHandler nicHandler;

    private final AliyunSecurityGroupHandler securityGroupHandler;

    public AliyunNicToSecurityGroupHandler(AliyunNicHandler nicHandler,
                                           AliyunSecurityGroupHandler securityGroupHandler) {
        this.nicHandler = nicHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_NIC_TO_SECURITY_GROUP_RELATIONSHIP";
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
        AliyunCloudProvider provider = (AliyunCloudProvider) nicHandler.getProvider();

        AliyunClient client = provider.buildClient(account);

        JoinSecurityGroupRequest request = new JoinSecurityGroupRequest();
        request.setNetworkInterfaceId(nic.getExternalId());
        request.setSecurityGroupId(securityGroup.getExternalId());

        client.ecs().joinSecurityGroup(request);
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource nic = relationship.getSource();
        Resource securityGroup = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(nic.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) nicHandler.getProvider();

        Optional<AliyunSecurityGroup> optionalSg = securityGroupHandler.describeSecurityGroup(
                account, securityGroup.getExternalId()
        );

        Optional<AliyunNic> optionalAliyunNic = nicHandler.describeNic(account, nic.getExternalId());

        if(optionalSg.isEmpty())
            return;

        if(optionalAliyunNic.isEmpty())
            return;

        List<String> securityGroupIds = optionalAliyunNic.get().detail().getSecurityGroupIds().getSecurityGroupId();
        if(Utils.length(securityGroupIds) <= 1){
            log.warn("Cannot detach the only security group: {}", securityGroupIds);
            return;
        }

        AliyunClient client = provider.buildClient(account);

        LeaveSecurityGroupRequest request = new LeaveSecurityGroupRequest();
        request.setSecurityGroupId(securityGroup.getExternalId());
        request.setNetworkInterfaceId(nic.getExternalId());

        client.ecs().leaveSecurityGroup(request);
    }

    @Override
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        Resource nic = relationship.getSource();

        Optional<AliyunNic> optionalAliyunNic = nicHandler.describeNic(account, nic.getExternalId());

        if(optionalAliyunNic.isPresent() &&
                Utils.length(optionalAliyunNic.get().detail().getSecurityGroupIds().getSecurityGroupId()) <= 1)
            return RelationshipActionResult.finished();

        return RelationshipHandler.super.checkDisconnectResult(account, relationship);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunNic> nic = nicHandler.describeNic(account, source.externalId());

        if(nic.isEmpty())
            return List.of();

        var securityGroupIds = nic.get().detail().getSecurityGroupIds();
        if(securityGroupIds == null || Utils.isEmpty(securityGroupIds.getSecurityGroupId()))
            return List.of();


        List<ExternalRequirement> result = new ArrayList<>();
        for (String groupId : nic.get().detail().getSecurityGroupIds().getSecurityGroupId()) {
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
