package com.stratocloud.resource.cmd.ownership;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class TransferCmd implements ApiCommand {
    private Long resourceId;
    private Long newOwnerId;
    private Long newTenantId;
    private Boolean enableCascadedTransfer = true;
}
