package com.stratocloud.provider.huawei.securitygroup.requirements;

import com.huaweicloud.sdk.vpc.v2.model.SecurityGroupRule;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupRuleHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public abstract class HuaweiRuleToSecurityGroupHandler implements EssentialRequirementHandler {

    private final HuaweiSecurityGroupRuleHandler ruleHandler;

    private final HuaweiSecurityGroupHandler securityGroupHandler;

    public HuaweiRuleToSecurityGroupHandler(HuaweiSecurityGroupRuleHandler ruleHandler,
                                            HuaweiSecurityGroupHandler securityGroupHandler) {
        this.ruleHandler = ruleHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeName() {
        return "安全组与规则";
    }

    @Override
    public ResourceHandler getSource() {
        return ruleHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return securityGroupHandler;
    }


    @Override
    public String getRequirementName() {
        return "安全组";
    }

    @Override
    public String getConnectActionName() {
        return "添加";
    }

    @Override
    public String getDisconnectActionName() {
        return "移除";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<SecurityGroupRule> rule = ruleHandler.describeSecurityGroupRule(account, source.externalId());

        if(rule.isEmpty())
            return List.of();

        Optional<ExternalResource> securityGroup = securityGroupHandler.describeExternalResource(
                account, rule.get().getSecurityGroupId()
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
