package com.stratocloud.user;

import com.stratocloud.identity.SimpleUser;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.user.query.DescribeUsersSimpleResponse;
import com.stratocloud.user.query.UserResponse;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserAssembler {

    public DescribeUsersSimpleResponse toSimpleUsersResponse(List<User> users) {
        if(Utils.isEmpty(users))
            return new DescribeUsersSimpleResponse(List.of());
        return new DescribeUsersSimpleResponse(users.stream().map(this::toSimpleUserResponse).toList());
    }

    private SimpleUser toSimpleUserResponse(User user) {
        return new SimpleUser(user.getId(), user.getLoginName(), user.getRealName());
    }

    public static UserResponse toUserResponse(User user) {
        UserResponse userResponse = new UserResponse();

        EntityUtil.copyBasicFields(user, userResponse);

        userResponse.setLoginName(user.getLoginName());
        userResponse.setRealName(user.getRealName());
        userResponse.setEmailAddress(user.getEmailAddress());
        userResponse.setPhoneNumber(user.getPhoneNumber());
        userResponse.setIconId(user.getIconId());
        userResponse.setDescription(user.getDescription());
        userResponse.setAuthType(user.getAuthType());
        userResponse.setDisabled(user.getDisabled());
        userResponse.setLocked(user.getLocked());
        userResponse.setLastLoginTime(user.getLastLoginTime());
        userResponse.setPasswordExpireTime(user.getPasswordExpireTime());

        userResponse.setRoleIds(
                user.getUserRoles().stream().map(ur->ur.getRole().getId()).collect(Collectors.toList())
        );

        return userResponse;
    }
}
