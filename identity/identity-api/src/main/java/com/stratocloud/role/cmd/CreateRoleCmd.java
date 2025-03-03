package com.stratocloud.role.cmd;

import com.stratocloud.identity.RoleType;
import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class CreateRoleCmd implements ApiCommand {
    private Long tenantId;
    private RoleType roleType;
    private String name;
    private String description;
}
