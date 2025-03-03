package com.stratocloud.tenant.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class CreateTenantCmd implements ApiCommand {
    private String name;
    private String description;
    private Long parentId;
}
