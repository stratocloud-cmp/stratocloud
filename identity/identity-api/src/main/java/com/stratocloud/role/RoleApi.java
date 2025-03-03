package com.stratocloud.role;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.role.cmd.*;
import com.stratocloud.role.query.DescribePermissionsRequest;
import com.stratocloud.role.query.DescribePermissionsResponse;
import com.stratocloud.role.query.DescribeRolesRequest;
import com.stratocloud.role.query.NestedRoleResponse;
import com.stratocloud.role.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface RoleApi {
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/create-role")
    CreateRoleResponse createRole(@RequestBody CreateRoleCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/update-role")
    UpdateRoleResponse updateRole(@RequestBody UpdateRoleCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/delete-roles")
    DeleteRolesResponse deleteRoles(@RequestBody DeleteRolesCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/add-permissions-to-role")
    AddPermissionsToRoleResponse addPermissionsToRole(@RequestBody AddPermissionsToRoleCmd cmd);
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/remove-permissions-from-role")
    RemovePermissionsFromRoleResponse removePermissionsFromRole(@RequestBody RemovePermissionsFromRoleCmd cmd);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/describe-roles")
    Page<NestedRoleResponse> describeRoles(@RequestBody DescribeRolesRequest request);

    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/describe-permissions")
    DescribePermissionsResponse describePermissions(@RequestBody DescribePermissionsRequest request);
}
