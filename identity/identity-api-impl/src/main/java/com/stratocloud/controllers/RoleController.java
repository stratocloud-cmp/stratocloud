package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.permission.*;
import com.stratocloud.role.RoleApi;
import com.stratocloud.role.RoleService;
import com.stratocloud.role.cmd.*;
import com.stratocloud.role.query.DescribePermissionsRequest;
import com.stratocloud.role.query.DescribePermissionsResponse;
import com.stratocloud.role.query.DescribeRolesRequest;
import com.stratocloud.role.query.NestedRoleResponse;
import com.stratocloud.role.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "Role", targetName = "角色")
@RestController
public class RoleController implements RoleApi {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateRole",
            actionName = "创建角色",
            objectType = "Role",
            objectTypeName = "角色"
    )
    public CreateRoleResponse createRole(@RequestBody CreateRoleCmd cmd) {
        return roleService.createRole(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateRole",
            actionName = "更新角色",
            objectType = "Role",
            objectTypeName = "角色"
    )
    public UpdateRoleResponse updateRole(@RequestBody UpdateRoleCmd cmd) {
        return roleService.updateRole(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteRoles",
            actionName = "删除角色",
            objectType = "Role",
            objectTypeName = "角色"
    )
    public DeleteRolesResponse deleteRoles(@RequestBody DeleteRolesCmd cmd) {
        return roleService.deleteRoles(cmd);
    }


    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "AddPermissionsToRole",
            actionName = "添加权限",
            objectType = "Role",
            objectTypeName = "角色"
    )
    public AddPermissionsToRoleResponse addPermissionsToRole(@RequestBody AddPermissionsToRoleCmd cmd) {
        return roleService.addPermissionsToRole(cmd);
    }


    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "RemovePermissionsFromRole",
            actionName = "移除权限",
            objectType = "Role",
            objectTypeName = "角色"
    )
    public RemovePermissionsFromRoleResponse removePermissionsFromRole(@RequestBody RemovePermissionsFromRoleCmd cmd) {
        return roleService.removePermissionsFromRole(cmd);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedRoleResponse> describeRoles(@RequestBody DescribeRolesRequest request) {
        return roleService.describeRoles(request);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public DescribePermissionsResponse describePermissions(@RequestBody DescribePermissionsRequest request) {
        return roleService.describePermissions(request);
    }
}
