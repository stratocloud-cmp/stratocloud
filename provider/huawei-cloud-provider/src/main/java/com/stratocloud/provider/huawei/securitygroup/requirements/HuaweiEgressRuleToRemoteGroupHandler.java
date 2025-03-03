package com.stratocloud.provider.huawei.securitygroup.requirements;

import com.stratocloud.provider.huawei.securitygroup.HuaweiEgressRuleHandler;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import org.springframework.stereotype.Component;

@Component
public class HuaweiEgressRuleToRemoteGroupHandler extends HuaweiRuleToRemoteGroupHandler {

    public static final String TYPE_ID = "HUAWEI_EGRESS_RULE_TO_REMOTE_GROUP_RELATIONSHIP";

    public HuaweiEgressRuleToRemoteGroupHandler(HuaweiEgressRuleHandler ruleHandler,
                                                HuaweiSecurityGroupHandler securityGroupHandler) {
        super(ruleHandler, securityGroupHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRequirementName() {
        return "目的端安全组";
    }

    @Override
    public String getCapabilityName() {
        return "源端出站规则";
    }
}
