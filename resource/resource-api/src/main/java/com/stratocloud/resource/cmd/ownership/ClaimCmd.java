package com.stratocloud.resource.cmd.ownership;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class ClaimCmd implements ApiCommand {
    private Long resourceId;
    private Boolean enableCascadedClaim = true;
}
