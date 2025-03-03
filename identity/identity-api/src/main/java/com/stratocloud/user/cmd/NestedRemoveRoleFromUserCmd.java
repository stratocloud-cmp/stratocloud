package com.stratocloud.user.cmd;

import lombok.Data;

@Data
public class NestedRemoveRoleFromUserCmd {
    private Long roleId;
    private Long userId;
}
