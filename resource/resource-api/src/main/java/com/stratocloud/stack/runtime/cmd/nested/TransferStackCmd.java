package com.stratocloud.stack.runtime.cmd.nested;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class TransferStackCmd implements ApiCommand {
    private Long stackId;
    private Long newOwnerId;
    private Long newTenantId;
}
