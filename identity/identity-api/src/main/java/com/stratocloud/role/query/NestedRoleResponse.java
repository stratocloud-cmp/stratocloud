package com.stratocloud.role.query;

import com.stratocloud.identity.RoleType;
import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedRoleResponse extends NestedTenanted {
    private RoleType type;
    private String name;
    private String description;

    private List<Long> permissionIds;
}
