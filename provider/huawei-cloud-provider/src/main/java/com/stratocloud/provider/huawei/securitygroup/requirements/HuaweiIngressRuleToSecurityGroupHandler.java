package com.stratocloud.provider.huawei.securitygroup.requirements;

import com.stratocloud.provider.huawei.securitygroup.HuaweiIngressRuleHandler;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import org.springframework.stereotype.Component;

@Component
public class HuaweiIngressRuleToSecurityGroupHandler extends HuaweiRuleToSecurityGroupHandler {

    public static final String TYPE_ID = "HUAWEI_INGRESS_RULE_TO_SECURITY_GROUP_RELATIONSHIP";

    public HuaweiIngressRuleToSecurityGroupHandler(HuaweiIngressRuleHandler ruleHandler,
                                                   HuaweiSecurityGroupHandler securityGroupHandler) {
        super(ruleHandler, securityGroupHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getCapabilityName() {
        return "入站规则";
    }
}
