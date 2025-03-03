package com.stratocloud.user;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.user.cmd.*;
import com.stratocloud.user.query.DescribeUsersRequest;
import com.stratocloud.user.query.DescribeUsersSimpleResponse;
import com.stratocloud.user.query.UserResponse;
import com.stratocloud.user.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserApi {
    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/describe-simple-users")
    DescribeUsersSimpleResponse describeSimpleUsers(@RequestBody DescribeUsersRequest request);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/describe-users")
    Page<UserResponse> describeUsers(@RequestBody DescribeUsersRequest request);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/create-user")
    CreateUserResponse createUser(@RequestBody CreateUserCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/update-user")
    UpdateUserResponse updateUser(@RequestBody UpdateUserCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/disable-users")
    DisableUsersResponse disableUsers(@RequestBody DisableUsersCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/enable-users")
    EnableUsersResponse enableUsers(@RequestBody EnableUsersCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/unlock-users")
    UnlockUsersResponse unlockUsers(@RequestBody UnlockUsersCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/delete-users")
    DeleteUsersResponse deleteUsers(@RequestBody DeleteUsersCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/batch-assign-role-to-user")
    BatchAssignRoleToUserResponse batchAssignRoleToUser(@RequestBody BatchAssignRoleToUserCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/batch-remove-role-from-user")
    BatchRemoveRoleFromUserResponse batchRemoveRoleFromUser(@RequestBody BatchRemoveRoleFromUserCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/change-password")
    ChangePasswordResponse changePassword(@RequestBody ChangePasswordCmd cmd);
}
