package com.stratocloud.provider.aliyun.securitygroup.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroup;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroupHandler;
import com.stratocloud.provider.aliyun.vpc.AliyunVpcHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunSecurityGroupToVpcHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "ALIYUN_SECURITY_GROUP_TO_VPC_RELATIONSHIP";
    private final AliyunSecurityGroupHandler securityGroupHandler;

    private final AliyunVpcHandler vpcHandler;

    public AliyunSecurityGroupToVpcHandler(AliyunSecurityGroupHandler securityGroupHandler,
                                           AliyunVpcHandler vpcHandler) {
        this.securityGroupHandler = securityGroupHandler;
        this.vpcHandler = vpcHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "安全组与私有网络";
    }

    @Override
    public ResourceHandler getSource() {
        return securityGroupHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return vpcHandler;
    }

    @Override
    public String getCapabilityName() {
        return "安全组";
    }

    @Override
    public String getRequirementName() {
        return "私有网络";
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
        Optional<AliyunSecurityGroup> securityGroup = securityGroupHandler.describeSecurityGroup(
                account, source.externalId()
        );

        if(securityGroup.isEmpty())
            return List.of();

        Optional<ExternalResource> vpc = vpcHandler.describeExternalResource(
                account, securityGroup.get().detail().getVpcId()
        );

        if(vpc.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                vpc.get(),
                Map.of()
        ));
    }

}
