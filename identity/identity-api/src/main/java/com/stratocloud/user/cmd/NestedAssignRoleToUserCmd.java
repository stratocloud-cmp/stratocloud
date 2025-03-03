package com.stratocloud.user.cmd;

import lombok.Data;

import java.util.List;

@Data
public class NestedAssignRoleToUserCmd {
    private Long roleId;
    private Long userId;
    private List<Long> grantedTenantIds;
}
