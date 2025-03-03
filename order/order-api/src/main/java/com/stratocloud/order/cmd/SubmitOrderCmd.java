package com.stratocloud.order.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class SubmitOrderCmd implements ApiCommand {
    private Long orderId;
    private String message;
}
