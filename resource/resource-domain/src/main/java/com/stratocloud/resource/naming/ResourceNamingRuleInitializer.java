package com.stratocloud.resource.naming;

import com.stratocloud.rule.SuffixPolicy;
import com.stratocloud.rule.NamingRuleInitializer;
import com.stratocloud.rule.SuffixType;
import org.springframework.stereotype.Component;

@Component
public class ResourceNamingRuleInitializer implements NamingRuleInitializer {

    @Override
    public String getRuleType() {
        return ResourceRuleTypes.RESOURCE_NAMING_RULE;
    }

    @Override
    public String getRuleName() {
        return "资源命名规则";
    }

    @Override
    public String getDefaultScriptPath() {
        return "classpath:scripts/ResourceNamingRule.js";
    }

    @Override
    public SuffixPolicy getSuffixPolicy() {
        return new SuffixPolicy(SuffixType.DYNAMIC_NUMERIC_SEQUENCE, 4, 1);
    }
}
