package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.permission.*;
import com.stratocloud.script.ScriptDefinitionApi;
import com.stratocloud.script.ScriptDefinitionService;
import com.stratocloud.script.cmd.*;
import com.stratocloud.script.query.DescribeScriptDefinitionsRequest;
import com.stratocloud.script.query.NestedScriptDefinitionResponse;
import com.stratocloud.script.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "ScriptDefinition", targetName = "脚本库")
@RestController
public class ScriptDefinitionController implements ScriptDefinitionApi {

    private final ScriptDefinitionService service;

    public ScriptDefinitionController(ScriptDefinitionService service) {
        this.service = service;
    }

    @ReadPermissionRequired
    @Override
    public Page<NestedScriptDefinitionResponse> describeScriptDefinitions(@RequestBody DescribeScriptDefinitionsRequest request) {
        return service.describeScriptDefinitions(request);
    }

    @CreatePermissionRequired
    @Override
    @SendAuditLog(
            action = "CreateScriptDefinition",
            actionName = "创建脚本定义",
            objectType = "ScriptDefinition",
            objectTypeName = "脚本定义"
    )
    public CreateScriptDefinitionResponse createScriptDefinition(@RequestBody CreateScriptDefinitionCmd cmd) {
        return service.createScriptDefinition(cmd);
    }

    @UpdatePermissionRequired
    @Override
    @SendAuditLog(
            action = "UpdateScriptDefinition",
            actionName = "更新脚本定义",
            objectType = "ScriptDefinition",
            objectTypeName = "脚本定义"
    )
    public UpdateScriptDefinitionResponse updateScriptDefinition(@RequestBody UpdateScriptDefinitionCmd cmd) {
        return service.updateScriptDefinition(cmd);
    }

    @DeletePermissionRequired
    @Override
    @SendAuditLog(
            action = "DeleteScriptDefinitions",
            actionName = "删除脚本定义",
            objectType = "ScriptDefinition",
            objectTypeName = "脚本定义"
    )
    public DeleteScriptDefinitionsResponse deleteScriptDefinitions(@RequestBody DeleteScriptDefinitionsCmd cmd) {
        return service.deleteScriptDefinitions(cmd);
    }

    @UpdatePermissionRequired
    @Override
    @SendAuditLog(
            action = "EnableScriptDefinitions",
            actionName = "启用脚本定义",
            objectType = "ScriptDefinition",
            objectTypeName = "脚本定义"
    )
    public EnableScriptDefinitionsResponse enableScriptDefinitions(@RequestBody EnableScriptDefinitionsCmd cmd) {
        return service.enableScriptDefinitions(cmd);
    }


    @UpdatePermissionRequired
    @Override
    @SendAuditLog(
            action = "DisableScriptDefinitions",
            actionName = "停用脚本定义",
            objectType = "ScriptDefinition",
            objectTypeName = "脚本定义"
    )
    public DisableScriptDefinitionsResponse disableScriptDefinitions(@RequestBody DisableScriptDefinitionsCmd cmd) {
        return service.disableScriptDefinitions(cmd);
    }
}
