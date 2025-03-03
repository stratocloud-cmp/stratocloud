package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.permission.*;
import com.stratocloud.rule.*;
import com.stratocloud.rule.cmd.*;
import com.stratocloud.rule.query.DescribeRulesRequest;
import com.stratocloud.rule.query.NestedRuleResponse;
import com.stratocloud.rule.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "Rule", targetName = "规则管理")
@RestController
public class RuleController implements RuleApi {

    private final RuleService service;

    public RuleController(RuleService service) {
        this.service = service;
    }

    @Override
    public ExecuteRuleResponse executeRule(@RequestBody ExecuteRuleCmd cmd) {
        return service.executeRule(cmd);
    }

    @Override
    public ExecuteNamingRuleResponse executeNamingRule(@RequestBody ExecuteRuleCmd cmd) {
        return service.executeNamingRule(cmd);
    }

    @Override
    @ReadPermissionRequired
    public Page<NestedRuleResponse> describeRules(@RequestBody DescribeRulesRequest request) {
        return service.describeRules(request);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateRule",
            actionName = "更新规则",
            objectType = "Rule",
            objectTypeName = "规则"
    )
    public UpdateRuleResponse updateRule(@RequestBody UpdateRuleCmd cmd) {
        return service.updateRule(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateNamingRule",
            actionName = "更新命名规则",
            objectType = "NamingRule",
            objectTypeName = "命名规则"
    )
    public UpdateRuleResponse updateNamingRule(@RequestBody UpdateNamingRuleCmd cmd) {
        return service.updateNamingRule(cmd);
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateRule",
            actionName = "创建规则",
            objectType = "Rule",
            objectTypeName = "规则"
    )
    public CreateRuleResponse createRule(@RequestBody CreateRuleCmd cmd) {
        return service.createRule(cmd);
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateNamingRule",
            actionName = "创建命名规则",
            objectType = "NamingRule",
            objectTypeName = "命名规则"
    )
    public CreateRuleResponse createNamingRule(@RequestBody CreateNamingRuleCmd cmd) {
        return service.createNamingRule(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteRules",
            actionName = "删除规则",
            objectType = "Rule",
            objectTypeName = "规则"
    )
    public DeleteRulesResponse deleteRules(@RequestBody DeleteRulesCmd cmd) {
        return service.deleteRules(cmd);
    }
}
