package com.stratocloud.provider.huawei.securitygroup;

import com.huaweicloud.sdk.vpc.v2.model.SecurityGroupRule;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.ResourceCategory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class HuaweiIngressRuleHandler extends HuaweiSecurityGroupRuleHandler {

    public HuaweiIngressRuleHandler(HuaweiCloudProvider provider) {
        super(provider);
    }

    @Override
    protected boolean filterRule(SecurityGroupRule rule) {
        return Objects.equals(rule.getDirection(), "ingress");
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_INGRESS_RULE";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云入站规则";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SECURITY_GROUP_INGRESS_POLICY;
    }
}
