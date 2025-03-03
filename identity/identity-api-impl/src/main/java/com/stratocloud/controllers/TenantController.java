package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.permission.*;
import com.stratocloud.tenant.TenantApi;
import com.stratocloud.tenant.TenantService;
import com.stratocloud.tenant.cmd.*;
import com.stratocloud.tenant.query.*;
import com.stratocloud.tenant.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target="Tenant", targetName = "租户")
@RestController
public class TenantController implements TenantApi {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateTenant",
            actionName = "创建租户",
            objectType = "Tenant",
            objectTypeName = "租户"
    )
    public CreateTenantResponse createTenant(@RequestBody CreateTenantCmd cmd) {
        return tenantService.createTenant(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateTenant",
            actionName = "更新租户",
            objectType = "Tenant",
            objectTypeName = "租户"
    )
    public UpdateTenantResponse updateTenant(@RequestBody UpdateTenantCmd cmd) {
        return tenantService.updateTenant(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "DisableTenants",
            actionName = "禁用租户",
            objectType = "Tenant",
            objectTypeName = "租户"
    )
    public DisableTenantsResponse disableTenants(@RequestBody DisableTenantsCmd cmd) {
        return tenantService.disableTenants(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "EnableTenants",
            actionName = "启用租户",
            objectType = "Tenant",
            objectTypeName = "租户"
    )
    public EnableTenantsResponse enableTenants(@RequestBody EnableTenantsCmd cmd) {
        return tenantService.enableTenants(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteTenants",
            actionName = "删除租户",
            objectType = "Tenant",
            objectTypeName = "租户"
    )
    public DeleteTenantsResponse deleteTenants(@RequestBody DeleteTenantsCmd cmd) {
        return tenantService.deleteTenants(cmd);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public DescribeSimpleTenantsResponse describeInheritedTenants(@RequestBody DescribeInheritedTenantsRequest request) {
        return tenantService.describeInheritedTenants(request);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public DescribeSimpleTenantsResponse describeSubTenants(@RequestBody DescribeSubTenantsRequest request) {
        return tenantService.describeSubTenants(request);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedTenantResponse> describeTenants(@RequestBody DescribeTenantsRequest request) {
        return tenantService.describeTenants(request);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public DescribeTenantsTreeResponse describeTenantsTree(@RequestBody DescribeTenantsTreeRequest request) {
        return tenantService.describeTenantsTree(request);
    }
}
