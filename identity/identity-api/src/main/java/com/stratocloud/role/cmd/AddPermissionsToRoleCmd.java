package com.stratocloud.role.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class AddPermissionsToRoleCmd implements ApiCommand {
    private Long roleId;
    private List<Long> permissionIds;
}
