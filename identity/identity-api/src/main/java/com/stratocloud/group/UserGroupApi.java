package com.stratocloud.group;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.group.cmd.*;
import com.stratocloud.group.query.DescribeGroupsRequest;
import com.stratocloud.group.query.DescribeSimpleGroupsResponse;
import com.stratocloud.group.query.NestedUserGroupResponse;
import com.stratocloud.group.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserGroupApi {
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/create-user-group")
    CreateUserGroupResponse createUserGroup(@RequestBody CreateUserGroupCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/update-user-group")
    UpdateUserGroupResponse updateUserGroup(@RequestBody UpdateUserGroupCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/delete-user-groups")
    DeleteUserGroupsResponse deleteUserGroups(@RequestBody DeleteUserGroupsCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/add-users-to-group")
    AddUsersToGroupResponse addUsersToGroup(@RequestBody AddUsersToGroupCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/remove-users-from-group")
    RemoveUserFromGroupResponse removeUsersFromGroup(@RequestBody RemoveUsersFromGroupCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/describe-simple-groups")
    DescribeSimpleGroupsResponse describeSimpleUserGroups(@RequestBody DescribeGroupsRequest request);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/describe-groups")
    Page<NestedUserGroupResponse> describeUserGroups(@RequestBody DescribeGroupsRequest request);
}
