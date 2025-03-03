package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.permission.*;
import com.stratocloud.script.SoftwareDefinitionApi;
import com.stratocloud.script.SoftwareDefinitionService;
import com.stratocloud.script.cmd.*;
import com.stratocloud.script.query.DescribeSoftwareDefinitionsRequest;
import com.stratocloud.script.query.NestedSoftwareDefinitionResponse;
import com.stratocloud.script.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "SoftwareDefinition", targetName = "软件库")
@RestController
public class SoftwareDefinitionController implements SoftwareDefinitionApi {

    private final SoftwareDefinitionService service;

    public SoftwareDefinitionController(SoftwareDefinitionService service) {
        this.service = service;
    }

    @ReadPermissionRequired
    @Override
    public Page<NestedSoftwareDefinitionResponse> describeSoftwareDefinitions(@RequestBody DescribeSoftwareDefinitionsRequest request) {
        return service.describeSoftwareDefinitions(request);
    }

    @CreatePermissionRequired
    @Override
    @SendAuditLog(
            action = "CreateSoftwareDefinition",
            actionName = "创建软件定义",
            objectType = "SoftwareDefinition",
            objectTypeName = "软件定义"
    )
    public CreateSoftwareDefinitionResponse createSoftwareDefinition(@RequestBody CreateSoftwareDefinitionCmd cmd) {
        return service.createSoftwareDefinition(cmd);
    }

    @UpdatePermissionRequired
    @Override
    @SendAuditLog(
            action = "UpdateSoftwareDefinition",
            actionName = "更新软件定义",
            objectType = "SoftwareDefinition",
            objectTypeName = "软件定义"
    )
    public UpdateSoftwareDefinitionResponse updateSoftwareDefinition(@RequestBody UpdateSoftwareDefinitionCmd cmd) {
        return service.updateSoftwareDefinition(cmd);
    }

    @DeletePermissionRequired
    @Override
    @SendAuditLog(
            action = "DeleteSoftwareDefinitions",
            actionName = "删除软件定义",
            objectType = "SoftwareDefinition",
            objectTypeName = "软件定义"
    )
    public DeleteSoftwareDefinitionsResponse deleteSoftwareDefinitions(@RequestBody DeleteSoftwareDefinitionsCmd cmd) {
        return service.deleteSoftwareDefinitions(cmd);
    }

    @UpdatePermissionRequired
    @Override
    @SendAuditLog(
            action = "EnableSoftwareDefinitions",
            actionName = "启用软件定义",
            objectType = "SoftwareDefinition",
            objectTypeName = "软件定义"
    )
    public EnableSoftwareDefinitionsResponse enableSoftwareDefinitions(@RequestBody EnableSoftwareDefinitionsCmd cmd) {
        return service.enableSoftwareDefinitions(cmd);
    }

    @UpdatePermissionRequired
    @Override
    @SendAuditLog(
            action = "DisableSoftwareDefinitions",
            actionName = "停用软件定义",
            objectType = "SoftwareDefinition",
            objectTypeName = "软件定义"
    )
    public DisableSoftwareDefinitionsResponse disableSoftwareDefinitions(@RequestBody DisableSoftwareDefinitionsCmd cmd) {
        return service.disableSoftwareDefinitions(cmd);
    }
}
