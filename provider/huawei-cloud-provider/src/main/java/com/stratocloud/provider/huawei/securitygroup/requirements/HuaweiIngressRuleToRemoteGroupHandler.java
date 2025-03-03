package com.stratocloud.provider.huawei.securitygroup.requirements;

import com.stratocloud.provider.huawei.securitygroup.HuaweiIngressRuleHandler;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import org.springframework.stereotype.Component;

@Component
public class HuaweiIngressRuleToRemoteGroupHandler extends HuaweiRuleToRemoteGroupHandler {

    public static final String TYPE_ID = "HUAWEI_INGRESS_RULE_TO_REMOTE_GROUP_RELATIONSHIP";

    public HuaweiIngressRuleToRemoteGroupHandler(HuaweiIngressRuleHandler ruleHandler,
                                                 HuaweiSecurityGroupHandler securityGroupHandler) {
        super(ruleHandler, securityGroupHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRequirementName() {
        return "源端安全组";
    }

    @Override
    public String getCapabilityName() {
        return "目的端入站规则";
    }
}
