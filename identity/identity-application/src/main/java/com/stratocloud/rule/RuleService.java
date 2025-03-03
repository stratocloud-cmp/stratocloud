package com.stratocloud.rule;

import com.stratocloud.rule.cmd.*;
import com.stratocloud.rule.query.DescribeRulesRequest;
import com.stratocloud.rule.query.NestedRuleResponse;
import com.stratocloud.rule.response.*;
import org.springframework.data.domain.Page;

public interface RuleService {
    ExecuteRuleResponse executeRule(ExecuteRuleCmd cmd);

    ExecuteNamingRuleResponse executeNamingRule(ExecuteRuleCmd cmd);

    Page<NestedRuleResponse> describeRules(DescribeRulesRequest request);

    UpdateRuleResponse updateRule(UpdateRuleCmd cmd);

    UpdateRuleResponse updateNamingRule(UpdateNamingRuleCmd cmd);

    CreateRuleResponse createRule(CreateRuleCmd cmd);

    CreateRuleResponse createNamingRule(CreateNamingRuleCmd cmd);

    DeleteRulesResponse deleteRules(DeleteRulesCmd cmd);
}
