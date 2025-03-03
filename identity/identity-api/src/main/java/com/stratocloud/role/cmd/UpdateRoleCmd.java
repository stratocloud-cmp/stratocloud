package com.stratocloud.role.cmd;

import com.stratocloud.identity.RoleType;
import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class UpdateRoleCmd implements ApiCommand {
    private Long roleId;
    private RoleType roleType;
    private String name;
    private String description;
}
