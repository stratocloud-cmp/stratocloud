package com.stratocloud.user;

import com.stratocloud.user.cmd.*;
import com.stratocloud.user.query.DescribeUsersRequest;
import com.stratocloud.user.query.DescribeUsersSimpleResponse;
import com.stratocloud.user.query.UserResponse;
import com.stratocloud.user.response.*;
import org.springframework.data.domain.Page;

public interface UserService {
    DescribeUsersSimpleResponse describeSimpleUsers(DescribeUsersRequest request);

    Page<UserResponse> describeUsers(DescribeUsersRequest request);

    CreateUserResponse createUser(CreateUserCmd cmd);

    UpdateUserResponse updateUser(UpdateUserCmd cmd);

    DisableUsersResponse disableUsers(DisableUsersCmd cmd);

    EnableUsersResponse enableUsers(EnableUsersCmd cmd);

    UnlockUsersResponse unlockUsers(UnlockUsersCmd cmd);

    BatchAssignRoleToUserResponse batchAssignRoleToUser(BatchAssignRoleToUserCmd cmd);

    BatchRemoveRoleFromUserResponse batchRemoveRoleFromUser(BatchRemoveRoleFromUserCmd cmd);

    DeleteUsersResponse deleteUsers(DeleteUsersCmd cmd);

    ChangePasswordResponse changePassword(ChangePasswordCmd cmd);
}
