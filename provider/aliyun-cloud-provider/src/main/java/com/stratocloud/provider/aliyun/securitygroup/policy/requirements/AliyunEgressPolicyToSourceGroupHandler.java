package com.stratocloud.provider.aliyun.securitygroup.policy.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroupHandler;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunEgressPolicyHandler;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunSecurityGroupPolicy;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunEgressPolicyToSourceGroupHandler implements EssentialRequirementHandler {

    private final AliyunEgressPolicyHandler policyHandler;

    private final AliyunSecurityGroupHandler securityGroupHandler;

    public AliyunEgressPolicyToSourceGroupHandler(AliyunEgressPolicyHandler policyHandler,
                                                  AliyunSecurityGroupHandler securityGroupHandler) {
        this.policyHandler = policyHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return AliyunEgressPolicyHandler.SOURCE_GROUP_REL_TYPE;
    }

    @Override
    public String getRelationshipTypeName() {
        return "出站规则与源安全组";
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
        return "出站规则";
    }

    @Override
    public String getRequirementName() {
        return "安全组";
    }

    @Override
    public String getConnectActionName() {
        return "添加出站规则";
    }

    @Override
    public String getDisconnectActionName() {
        return "移除出站规则";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<AliyunSecurityGroupPolicy> policy = policyHandler.describeEgressPolicy(account, source.externalId());

        if(policy.isEmpty())
            return List.of();

        Optional<ExternalResource> securityGroup = securityGroupHandler.describeExternalResource(
                account, policy.get().policyId().securityGroupId()
        );

        return securityGroup.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }


}
