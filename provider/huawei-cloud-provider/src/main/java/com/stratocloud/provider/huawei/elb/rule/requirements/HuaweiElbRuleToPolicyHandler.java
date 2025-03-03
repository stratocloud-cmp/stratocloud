package com.stratocloud.provider.huawei.elb.rule.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.policy.HuaweiElbPolicyHandler;
import com.stratocloud.provider.huawei.elb.rule.HuaweiElbRuleHandler;
import com.stratocloud.provider.huawei.elb.rule.HuaweiRule;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbRuleToPolicyHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "HUAWEI_ELB_RULE_TO_POLICY_RELATIONSHIP";
    private final HuaweiElbRuleHandler ruleHandler;

    private final HuaweiElbPolicyHandler policyHandler;

    public HuaweiElbRuleToPolicyHandler(HuaweiElbRuleHandler ruleHandler,
                                        HuaweiElbPolicyHandler policyHandler) {
        this.ruleHandler = ruleHandler;
        this.policyHandler = policyHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "转发策略与转发规则";
    }

    @Override
    public ResourceHandler getSource() {
        return ruleHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return policyHandler;
    }

    @Override
    public String getCapabilityName() {
        return "转发规则";
    }

    @Override
    public String getRequirementName() {
        return "转发策略";
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
        Optional<HuaweiRule> rule = ruleHandler.describeRule(account, source.externalId());

        if(rule.isEmpty())
            return List.of();

        var policy = policyHandler.describeExternalResource(account, rule.get().id().policyId());

        return policy.map(p -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        p,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
