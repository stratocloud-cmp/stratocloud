package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.permission.*;
import com.stratocloud.user.UserApi;
import com.stratocloud.user.UserService;
import com.stratocloud.user.cmd.*;
import com.stratocloud.user.query.DescribeUsersRequest;
import com.stratocloud.user.query.DescribeUsersSimpleResponse;
import com.stratocloud.user.query.UserResponse;
import com.stratocloud.user.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "User", targetName = "用户")
@RestController
public class UserController implements UserApi {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public DescribeUsersSimpleResponse describeSimpleUsers(@RequestBody DescribeUsersRequest request) {
        return service.describeSimpleUsers(request);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<UserResponse> describeUsers(@RequestBody DescribeUsersRequest request) {
        return service.describeUsers(request);
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateUser",
            actionName = "创建用户",
            objectType = "User",
            objectTypeName = "用户"
    )
    public CreateUserResponse createUser(@RequestBody CreateUserCmd cmd) {
        return service.createUser(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateUser",
            actionName = "更新用户",
            objectType = "User",
            objectTypeName = "用户"
    )
    public UpdateUserResponse updateUser(@RequestBody UpdateUserCmd cmd) {
        return service.updateUser(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "DisableUsers",
            actionName = "禁用用户",
            objectType = "User",
            objectTypeName = "用户"
    )
    public DisableUsersResponse disableUsers(@RequestBody DisableUsersCmd cmd) {
        return service.disableUsers(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "EnableUsers",
            actionName = "启用用户",
            objectType = "User",
            objectTypeName = "用户"
    )
    public EnableUsersResponse enableUsers(@RequestBody EnableUsersCmd cmd) {
        return service.enableUsers(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UnlockUsers",
            actionName = "解锁用户",
            objectType = "User",
            objectTypeName = "用户"
    )
    public UnlockUsersResponse unlockUsers(@RequestBody UnlockUsersCmd cmd) {
        return service.unlockUsers(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteUsers",
            actionName = "删除用户",
            objectType = "User",
            objectTypeName = "用户"
    )
    public DeleteUsersResponse deleteUsers(@RequestBody DeleteUsersCmd cmd) {
        return service.deleteUsers(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "BatchAssignRoleToUser",
            actionName = "关联角色",
            objectType = "User",
            objectTypeName = "用户"
    )
    public BatchAssignRoleToUserResponse batchAssignRoleToUser(@RequestBody BatchAssignRoleToUserCmd cmd) {
        return service.batchAssignRoleToUser(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "BatchRemoveRoleFromUser",
            actionName = "移除角色",
            objectType = "User",
            objectTypeName = "用户"
    )
    public BatchRemoveRoleFromUserResponse batchRemoveRoleFromUser(@RequestBody BatchRemoveRoleFromUserCmd cmd) {
        return service.batchRemoveRoleFromUser(cmd);
    }


    @Override
    @SendAuditLog(
            action = "ChangePassword",
            actionName = "修改密码",
            objectType = "User",
            objectTypeName = "用户",
            hideRequestBody = true
    )
    public ChangePasswordResponse changePassword(@RequestBody ChangePasswordCmd cmd) {
        return service.changePassword(cmd);
    }
}
