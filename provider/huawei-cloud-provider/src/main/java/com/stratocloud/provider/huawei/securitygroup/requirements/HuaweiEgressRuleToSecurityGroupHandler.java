package com.stratocloud.provider.huawei.securitygroup.requirements;

import com.stratocloud.provider.huawei.securitygroup.HuaweiEgressRuleHandler;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import org.springframework.stereotype.Component;

@Component
public class HuaweiEgressRuleToSecurityGroupHandler extends HuaweiRuleToSecurityGroupHandler {

    public static final String TYPE_ID = "HUAWEI_EGRESS_RULE_TO_SECURITY_GROUP_RELATIONSHIP";

    public HuaweiEgressRuleToSecurityGroupHandler(HuaweiEgressRuleHandler ruleHandler,
                                                  HuaweiSecurityGroupHandler securityGroupHandler) {
        super(ruleHandler, securityGroupHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getCapabilityName() {
        return "出站规则";
    }
}
