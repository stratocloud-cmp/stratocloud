package com.stratocloud.group;

import com.stratocloud.group.query.DescribeSimpleGroupsResponse;
import com.stratocloud.group.query.NestedUserGroupResponse;
import com.stratocloud.group.query.NestedUserGroupTag;
import com.stratocloud.group.query.SimpleUserGroup;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.user.User;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserGroupAssembler {
    public DescribeSimpleGroupsResponse toSimpleGroupsResponse(List<UserGroup> userGroups) {
        List<SimpleUserGroup> simpleUserGroups = userGroups.stream().map(
                userGroup -> new SimpleUserGroup(userGroup.getId(), userGroup.getName())
        ).toList();
        return new DescribeSimpleGroupsResponse(simpleUserGroups);
    }

    public NestedUserGroupResponse toNestedUserGroupResponse(UserGroup userGroup) {
        NestedUserGroupResponse response = new NestedUserGroupResponse();

        EntityUtil.copyBasicFields(userGroup, response);

        response.setName(userGroup.getName());
        response.setAlias(userGroup.getAlias());
        response.setDescription(userGroup.getDescription());
        response.setTags(toNestedUserGroupTag(userGroup.getTags()));


        return response;
    }

    private List<NestedUserGroupTag> toNestedUserGroupTag(List<UserGroupTag> tags) {
        List<NestedUserGroupTag> result = new ArrayList<>();
        if(Utils.isEmpty(tags))
            return result;

        for (UserGroupTag tag : tags) {
            NestedUserGroupTag groupTag = new NestedUserGroupTag();
            groupTag.setTagKey(tag.getTagKey());
            groupTag.setTagKeyName(tag.getTagKeyName());
            groupTag.setTagValue(tag.getTagValue());
            groupTag.setTagValueName(tag.getTagValueName());
            result.add(groupTag);
        }
        return result;
    }
}
