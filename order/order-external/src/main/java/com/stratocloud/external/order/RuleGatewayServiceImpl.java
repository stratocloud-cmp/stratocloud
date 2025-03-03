package com.stratocloud.external.order;

import com.stratocloud.rule.response.ExecuteNamingRuleResponse;
import com.stratocloud.rule.cmd.ExecuteRuleCmd;
import com.stratocloud.rule.RuleApi;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("ruleGatewayForOrder")
public class RuleGatewayServiceImpl implements RuleGatewayService{

    private final RuleApi ruleApi;

    public RuleGatewayServiceImpl(RuleApi ruleApi) {
        this.ruleApi = ruleApi;
    }

    @Override
    public String executeNamingRule(String ruleType, Map<String, Object> args) {
        ExecuteRuleCmd executeRuleCmd = new ExecuteRuleCmd();
        executeRuleCmd.setRuleType(ruleType);
        executeRuleCmd.setParameters(args);

        ExecuteNamingRuleResponse response = ruleApi.executeNamingRule(executeRuleCmd);

        return response.getNextName();
    }
}
