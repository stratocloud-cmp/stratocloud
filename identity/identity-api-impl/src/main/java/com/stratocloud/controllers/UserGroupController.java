package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.group.UserGroupApi;
import com.stratocloud.group.UserGroupService;
import com.stratocloud.group.cmd.*;
import com.stratocloud.group.query.DescribeGroupsRequest;
import com.stratocloud.group.query.DescribeSimpleGroupsResponse;
import com.stratocloud.group.query.NestedUserGroupResponse;
import com.stratocloud.group.response.*;
import com.stratocloud.permission.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "UserGroup", targetName = "用户组")
@RestController
public class UserGroupController implements UserGroupApi {

    private final UserGroupService userGroupService;

    public UserGroupController(UserGroupService userGroupService) {
        this.userGroupService = userGroupService;
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateUserGroup",
            actionName = "创建用户组",
            objectType = "UserGroup",
            objectTypeName = "用户组"
    )
    public CreateUserGroupResponse createUserGroup(@RequestBody CreateUserGroupCmd cmd) {
        return userGroupService.createUserGroup(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateUserGroup",
            actionName = "更新用户组",
            objectType = "UserGroup",
            objectTypeName = "用户组"
    )
    public UpdateUserGroupResponse updateUserGroup(@RequestBody UpdateUserGroupCmd cmd) {
        return userGroupService.updateUserGroup(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteUserGroups",
            actionName = "删除用户组",
            objectType = "UserGroup",
            objectTypeName = "用户组"
    )
    public DeleteUserGroupsResponse deleteUserGroups(@RequestBody DeleteUserGroupsCmd cmd) {
        return userGroupService.deleteUserGroups(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "AddUsersToGroup",
            actionName = "用户组添加用户",
            objectType = "UserGroup",
            objectTypeName = "用户组"
    )
    public AddUsersToGroupResponse addUsersToGroup(@RequestBody AddUsersToGroupCmd cmd) {
        return userGroupService.addUsersToGroup(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "RemoveUsersFromGroup",
            actionName = "用户组移除用户",
            objectType = "UserGroup",
            objectTypeName = "用户组"
    )
    public RemoveUserFromGroupResponse removeUsersFromGroup(@RequestBody RemoveUsersFromGroupCmd cmd) {
        return userGroupService.removeUsersFromGroup(cmd);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public DescribeSimpleGroupsResponse describeSimpleUserGroups(@RequestBody DescribeGroupsRequest request) {
        return userGroupService.describeSimpleUserGroups(request);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedUserGroupResponse> describeUserGroups(@RequestBody DescribeGroupsRequest request) {
        return userGroupService.describeUserGroups(request);
    }
}
