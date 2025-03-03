package com.stratocloud.tenant.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class UpdateTenantCmd implements ApiCommand {
    private Long tenantId;
    private String name;
    private String description;
}
