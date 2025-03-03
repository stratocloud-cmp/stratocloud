package com.stratocloud.provider.huawei.securitygroup.requirements;

import com.huaweicloud.sdk.vpc.v2.model.SecurityGroupRule;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupRuleHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


public abstract class HuaweiRuleToRemoteGroupHandler implements ExclusiveRequirementHandler {

    private final HuaweiSecurityGroupRuleHandler ruleHandler;

    private final HuaweiSecurityGroupHandler securityGroupHandler;

    public HuaweiRuleToRemoteGroupHandler(HuaweiSecurityGroupRuleHandler ruleHandler,
                                          HuaweiSecurityGroupHandler securityGroupHandler) {
        this.ruleHandler = ruleHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeName() {
        return "安全组规则与远端安全组";
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
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<SecurityGroupRule> rule = ruleHandler.describeSecurityGroupRule(account, source.externalId());

        if(rule.isEmpty())
            return List.of();

        if(Objects.equals(rule.get().getRemoteGroupId(), rule.get().getSecurityGroupId()))
            return List.of();

        Optional<ExternalResource> remoteGroup = securityGroupHandler.describeExternalResource(
                account, rule.get().getRemoteGroupId()
        );

        return remoteGroup.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
