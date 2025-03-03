package com.stratocloud.group;

import com.stratocloud.group.cmd.*;
import com.stratocloud.group.query.DescribeGroupsRequest;
import com.stratocloud.group.query.DescribeSimpleGroupsResponse;
import com.stratocloud.group.query.NestedUserGroupResponse;
import com.stratocloud.group.response.*;
import org.springframework.data.domain.Page;

public interface UserGroupService {
    CreateUserGroupResponse createUserGroup(CreateUserGroupCmd cmd);

    UpdateUserGroupResponse updateUserGroup(UpdateUserGroupCmd cmd);

    DeleteUserGroupsResponse deleteUserGroups(DeleteUserGroupsCmd cmd);

    AddUsersToGroupResponse addUsersToGroup(AddUsersToGroupCmd cmd);

    RemoveUserFromGroupResponse removeUsersFromGroup(RemoveUsersFromGroupCmd cmd);

    DescribeSimpleGroupsResponse describeSimpleUserGroups(DescribeGroupsRequest request);

    Page<NestedUserGroupResponse> describeUserGroups(DescribeGroupsRequest request);
}
