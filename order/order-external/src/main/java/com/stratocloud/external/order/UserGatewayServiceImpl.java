package com.stratocloud.external.order;

import com.stratocloud.auth.CallContext;
import com.stratocloud.identity.SimpleUser;
import com.stratocloud.group.UserGroupApi;
import com.stratocloud.group.query.DescribeGroupsRequest;
import com.stratocloud.group.query.SimpleUserGroup;
import com.stratocloud.user.query.DescribeUsersRequest;
import com.stratocloud.user.query.DescribeUsersSimpleResponse;
import com.stratocloud.user.UserApi;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("userGatewayForOrder")
public class UserGatewayServiceImpl implements UserGatewayService {

    private final UserApi userApi;

    private final UserGroupApi userGroupApi;

    public UserGatewayServiceImpl(UserApi userApi, UserGroupApi userGroupApi) {
        this.userApi = userApi;
        this.userGroupApi = userGroupApi;
    }

    @Override
    public List<SimpleUser> findUsers(List<Long> userIds) {
        DescribeUsersRequest request = new DescribeUsersRequest();
        request.setUserIds(userIds);
        DescribeUsersSimpleResponse response = userApi.describeSimpleUsers(request);

        if(response.getUsers() == null)
            return List.of();

        return response.getUsers();
    }

    @Override
    public List<SimpleUser> findHandlersByRolesFromTenant(List<Long> roleIds) {
        DescribeUsersRequest request = new DescribeUsersRequest();
        request.setRoleIds(roleIds);
        DescribeUsersSimpleResponse response = userApi.describeSimpleUsers(request);

        if(response.getUsers() == null)
            return List.of();

        return response.getUsers();
    }

    @Override
    public List<SimpleUser> findHandlersByRolesFromUserGroups(List<Long> roleIds) {
        Long callerId = CallContext.current().getCallingUser().userId();

        DescribeGroupsRequest describeGroupsRequest = new DescribeGroupsRequest();
        describeGroupsRequest.setUserIds(List.of(callerId));
        List<SimpleUserGroup> groups = userGroupApi.describeSimpleUserGroups(describeGroupsRequest).getGroups();

        if(groups == null)
            return List.of();

        List<Long> userGroupIds = groups.stream().map(SimpleUserGroup::getUserGroupId).toList();

        DescribeUsersRequest describeUsersRequest = new DescribeUsersRequest();
        describeUsersRequest.setRoleIds(roleIds);
        describeUsersRequest.setUserGroupIds(userGroupIds);
        DescribeUsersSimpleResponse response = userApi.describeSimpleUsers(describeUsersRequest);

        if(response.getUsers() == null)
            return List.of();

        return response.getUsers();
    }
}
