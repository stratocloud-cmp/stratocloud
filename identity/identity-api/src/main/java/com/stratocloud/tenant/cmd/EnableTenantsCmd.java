package com.stratocloud.tenant.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class EnableTenantsCmd implements ApiCommand {
    private List<Long> tenantIds;
}
