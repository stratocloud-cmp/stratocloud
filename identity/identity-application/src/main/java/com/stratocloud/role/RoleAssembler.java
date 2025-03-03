package com.stratocloud.role;

import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.permission.Permission;
import com.stratocloud.role.query.NestedPermission;
import com.stratocloud.role.query.NestedRoleResponse;
import org.springframework.stereotype.Component;

@Component
public class RoleAssembler {
    public NestedRoleResponse toNestedRoleResponse(Role role) {
        NestedRoleResponse response = new NestedRoleResponse();
        EntityUtil.copyBasicFields(role, response);
        response.setType(role.getType());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        response.setPermissionIds(role.getPermissions().stream().map(Permission::getId).distinct().toList());
        return response;
    }

    public NestedPermission toNestedPermission(Permission permission) {
        NestedPermission nestedPermission = new NestedPermission();
        EntityUtil.copyBasicFields(permission, nestedPermission);
        nestedPermission.setTarget(permission.getTarget());
        nestedPermission.setTargetName(permission.getTargetName());
        nestedPermission.setAction(permission.getAction());
        nestedPermission.setActionName(permission.getActionName());
        return nestedPermission;
    }
}
