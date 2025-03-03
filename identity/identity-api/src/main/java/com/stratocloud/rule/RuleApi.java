package com.stratocloud.rule;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.rule.cmd.*;
import com.stratocloud.rule.query.DescribeRulesRequest;
import com.stratocloud.rule.query.NestedRuleResponse;
import com.stratocloud.rule.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface RuleApi {
    @PostMapping(StratoServices.IDENTITY_SERVICE+"/execute-rule")
    ExecuteRuleResponse executeRule(@RequestBody ExecuteRuleCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/execute-naming-rule")
    ExecuteNamingRuleResponse executeNamingRule(@RequestBody ExecuteRuleCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/describe-rules")
    Page<NestedRuleResponse> describeRules(@RequestBody DescribeRulesRequest request);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/update-rule")
    UpdateRuleResponse updateRule(@RequestBody UpdateRuleCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/update-naming-rule")
    UpdateRuleResponse updateNamingRule(@RequestBody UpdateNamingRuleCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/create-rule")
    CreateRuleResponse createRule(@RequestBody CreateRuleCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/create-naming-rule")
    CreateRuleResponse createNamingRule(@RequestBody CreateNamingRuleCmd cmd);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/delete-rules")
    DeleteRulesResponse deleteRules(@RequestBody DeleteRulesCmd cmd);
}
