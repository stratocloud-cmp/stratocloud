package com.stratocloud.provider.tencent.securitygroup.policy.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.securitygroup.TencentSecurityGroupHandler;
import com.stratocloud.provider.tencent.securitygroup.policy.TencentSecurityGroupIngressPolicyHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroupPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentIngressPolicyToSecurityGroupHandler implements EssentialRequirementHandler {
    public static final String TYPE_ID = "TENCENT_INGRESS_POLICY_TO_SECURITY_GROUP_RELATIONSHIP";
    private final TencentSecurityGroupIngressPolicyHandler policyHandler;

    private final TencentSecurityGroupHandler securityGroupHandler;

    public TencentIngressPolicyToSecurityGroupHandler(TencentSecurityGroupIngressPolicyHandler policyHandler,
                                                      TencentSecurityGroupHandler securityGroupHandler) {
        this.policyHandler = policyHandler;
        this.securityGroupHandler = securityGroupHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "安全组与入站规则";
    }

    @Override
    public ResourceHandler getSource() {
        return policyHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return securityGroupHandler;
    }

    @Override
    public String getCapabilityName() {
        return "入站规则";
    }

    @Override
    public String getRequirementName() {
        return "安全组";
    }

    @Override
    public String getConnectActionName() {
        return "添加规则";
    }

    @Override
    public String getDisconnectActionName() {
        return "移除规则";
    }

    @Override
    public boolean synchronizeTarget() {
        return true;
    }


    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<SecurityGroupPolicy> policy = policyHandler.describeIngressPolicy(account, source.externalId());

        if(policy.isEmpty())
            return List.of();

        String securityGroupId = policy.get().getSecurityGroupId();

        Optional<ExternalResource> securityGroup
                = securityGroupHandler.describeExternalResource(account, securityGroupId);

        if(securityGroup.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                securityGroup.get(),
                Map.of()
        ));
    }
}
