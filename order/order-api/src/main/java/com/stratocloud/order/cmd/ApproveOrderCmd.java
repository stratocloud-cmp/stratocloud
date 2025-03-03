package com.stratocloud.order.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class ApproveOrderCmd implements ApiCommand {
    private Long orderId;
    private String message;
}
