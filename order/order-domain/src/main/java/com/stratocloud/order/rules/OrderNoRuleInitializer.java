package com.stratocloud.order.rules;

import com.stratocloud.rule.SuffixPolicy;
import com.stratocloud.rule.NamingRuleInitializer;
import com.stratocloud.rule.SuffixType;
import org.springframework.stereotype.Component;

@Component
public class OrderNoRuleInitializer implements NamingRuleInitializer {

    @Override
    public String getRuleType() {
        return OrderRuleTypes.ORDER_NO_RULE;
    }

    @Override
    public String getRuleName() {
        return "订单编号规则";
    }

    @Override
    public String getDefaultScriptPath() {
        return "classpath:scripts/OrderNoRule.js";
    }

    @Override
    public SuffixPolicy getSuffixPolicy() {
        return new SuffixPolicy(SuffixType.DYNAMIC_NUMERIC_SEQUENCE, 4, 1);
    }
}
