package com.stratocloud.order.rules;

import com.stratocloud.rule.SuffixPolicy;
import com.stratocloud.rule.NamingRuleInitializer;
import com.stratocloud.rule.SuffixType;
import org.springframework.stereotype.Component;

@Component
public class OrderNamingRuleInitializer implements NamingRuleInitializer {

    @Override
    public String getRuleType() {
        return OrderRuleTypes.ORDER_NAMING_RULE;
    }

    @Override
    public String getRuleName() {
        return "订单标题规则";
    }

    @Override
    public String getDefaultScriptPath() {
        return "classpath:scripts/OrderNamingRule.js";
    }

    @Override
    public SuffixPolicy getSuffixPolicy() {
        return new SuffixPolicy(SuffixType.DYNAMIC_NUMERIC_SEQUENCE, 4, 1);
    }
}
