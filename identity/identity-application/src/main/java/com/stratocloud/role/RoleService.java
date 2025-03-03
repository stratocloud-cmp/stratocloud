package com.stratocloud.role;

import com.stratocloud.role.cmd.*;
import com.stratocloud.role.query.DescribePermissionsRequest;
import com.stratocloud.role.query.DescribePermissionsResponse;
import com.stratocloud.role.query.DescribeRolesRequest;
import com.stratocloud.role.query.NestedRoleResponse;
import com.stratocloud.role.response.*;
import org.springframework.data.domain.Page;

public interface RoleService {
    CreateRoleResponse createRole(CreateRoleCmd cmd);

    UpdateRoleResponse updateRole(UpdateRoleCmd cmd);

    DeleteRolesResponse deleteRoles(DeleteRolesCmd cmd);

    AddPermissionsToRoleResponse addPermissionsToRole(AddPermissionsToRoleCmd cmd);

    RemovePermissionsFromRoleResponse removePermissionsFromRole(RemovePermissionsFromRoleCmd cmd);


    Page<NestedRoleResponse> describeRoles(DescribeRolesRequest request);

    DescribePermissionsResponse describePermissions(DescribePermissionsRequest request);
}
