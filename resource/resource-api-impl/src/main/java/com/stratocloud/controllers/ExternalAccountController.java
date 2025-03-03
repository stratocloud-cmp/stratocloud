package com.stratocloud.controllers;

import com.stratocloud.account.ExternalAccountApi;
import com.stratocloud.account.ExternalAccountService;
import com.stratocloud.account.cmd.*;
import com.stratocloud.account.query.DescribeAccountsRequest;
import com.stratocloud.account.query.NestedAccountResponse;
import com.stratocloud.account.response.*;
import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.permission.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "ExternalAccount", targetName = "云账号")
@RestController
public class ExternalAccountController implements ExternalAccountApi {

    private final ExternalAccountService service;

    public ExternalAccountController(ExternalAccountService service) {
        this.service = service;
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateExternalAccount",
            actionName = "创建云账号",
            objectType = "ExternalAccount",
            objectTypeName = "云账号",
            hideRequestBody = true
    )
    public CreateExternalAccountResponse createExternalAccount(@RequestBody CreateExternalAccountCmd cmd) {
        return service.createExternalAccount(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateExternalAccount",
            actionName = "更新云账号",
            objectType = "ExternalAccount",
            objectTypeName = "云账号",
            hideRequestBody = true
    )
    public UpdateExternalAccountResponse updateExternalAccount(@RequestBody UpdateExternalAccountCmd cmd) {
        return service.updateExternalAccount(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteExternalAccounts",
            actionName = "删除云账号",
            objectType = "ExternalAccount",
            objectTypeName = "云账号"
    )
    public DeleteExternalAccountsResponse deleteExternalAccounts(@RequestBody DeleteExternalAccountsCmd cmd) {
        return service.deleteExternalAccounts(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "EnableExternalAccounts",
            actionName = "启用云账号",
            objectType = "ExternalAccount",
            objectTypeName = "云账号"
    )
    public EnableAccountsResponse enableExternalAccounts(@RequestBody EnableAccountsCmd cmd) {
        return service.enableExternalAccounts(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "DisableExternalAccounts",
            actionName = "停用云账号",
            objectType = "ExternalAccount",
            objectTypeName = "云账号"
    )
    public DisableAccountsResponse disableExternalAccounts(@RequestBody DisableAccountsCmd cmd) {
        return service.disableExternalAccounts(cmd);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedAccountResponse> describeAccounts(@RequestBody DescribeAccountsRequest request) {
        return service.describeAccounts(request);
    }
}
