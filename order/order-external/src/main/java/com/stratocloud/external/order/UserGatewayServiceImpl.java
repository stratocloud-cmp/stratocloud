package com.stratocloud.external.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.stratocloud.auth.CallContext;
import com.stratocloud.group.UserGroupApi;
import com.stratocloud.group.cmd.NestedUserGroupTag;
import com.stratocloud.group.query.DescribeGroupsRequest;
import com.stratocloud.group.query.SimpleUserGroup;
import com.stratocloud.identity.SimpleUser;
import com.stratocloud.job.JobContext;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.user.UserApi;
import com.stratocloud.user.query.DescribeUsersRequest;
import com.stratocloud.user.query.DescribeUsersSimpleResponse;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
    public List<SimpleUser> findHandlersByRolesFromUserGroups(List<Long> roleIds, Map<String, Object> runtimeProperties) {
        Long callerId = CallContext.current().getCallingUser().userId();

        List<TagRecord> tags = JSON.convert(
                runtimeProperties.get(JobContext.KEY_RELATED_TAGS),
                new TypeReference<>(){}
        );

        DescribeGroupsRequest describeGroupsRequest = new DescribeGroupsRequest();
        List<SimpleUserGroup> groups;

        if(Utils.isEmpty(tags)){
            describeGroupsRequest.setUserIds(List.of(callerId));
        } else {
            describeGroupsRequest.setAllGroups(true);
            describeGroupsRequest.setTags(toNestedUserGroupTags(tags));
        }

        groups = userGroupApi.describeSimpleUserGroups(describeGroupsRequest).getGroups();


        if(Utils.isEmpty(groups))
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

    private List<NestedUserGroupTag> toNestedUserGroupTags(List<TagRecord> tags) {
        List<NestedUserGroupTag> result = new ArrayList<>();

        if(Utils.isNotEmpty(tags)){
            for (TagRecord tag : new HashSet<>(tags)) {
                NestedUserGroupTag nestedUserGroupTag = new NestedUserGroupTag();

                nestedUserGroupTag.setTagKey(tag.tagKey());
                nestedUserGroupTag.setTagKeyName(tag.tagKeyName());
                nestedUserGroupTag.setTagValue(tag.tagValue());
                nestedUserGroupTag.setTagValueName(tag.tagValueName());

                result.add(nestedUserGroupTag);
            }
        }

        return result;
    }
}
