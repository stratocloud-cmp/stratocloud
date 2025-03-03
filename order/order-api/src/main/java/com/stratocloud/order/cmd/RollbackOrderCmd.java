package com.stratocloud.order.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class RollbackOrderCmd implements ApiCommand {
    private Long orderId;
    private Long nodeId;
    private String message;
}
