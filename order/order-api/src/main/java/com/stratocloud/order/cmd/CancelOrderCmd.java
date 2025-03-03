package com.stratocloud.order.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class CancelOrderCmd implements ApiCommand {
    private Long orderId;
    private String message;
}
