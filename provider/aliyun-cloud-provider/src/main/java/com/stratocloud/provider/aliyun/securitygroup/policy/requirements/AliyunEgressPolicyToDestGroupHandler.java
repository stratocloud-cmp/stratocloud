package com.stratocloud.provider.aliyun.securitygroup.policy.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroupHandler;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunEgressPolicyHandler;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunSecurityGroupPolicy;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunEgressPolicyToDestGroupHandler implements ExclusiveRequirementHandler {

    private final AliyunEgressPolicyHandler policyHandler;

    private final AliyunSecurityGroupHandler securityGroupHandler;

    public AliyunEgressPolicyToDestGroupHandler(AliyunEgressPolicyHandler policyHandler,
                                                AliyunSecurityGroupHandler securityGroupHandler) {
        this.policyHandler = policyHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return AliyunEgressPolicyHandler.DEST_GROUP_REL_TYPE;
    }

    @Override
    public String getRelationshipTypeName() {
        return "出站规则与目的安全组";
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
        return "源端出站规则";
    }

    @Override
    public String getRequirementName() {
        return "目的安全组";
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<AliyunSecurityGroupPolicy> policy = policyHandler.describeEgressPolicy(account, source.externalId());

        if(policy.isEmpty())
            return List.of();

        Optional<ExternalResource> destGroup = securityGroupHandler.describeExternalResource(
                account, policy.get().detail().getDestGroupId()
        );

        if(destGroup.isEmpty())
            return List.of();

        return List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        destGroup.get(),
                        Map.of()
                )
        );
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
