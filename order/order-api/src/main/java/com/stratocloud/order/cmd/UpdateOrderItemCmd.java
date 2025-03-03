package com.stratocloud.order.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateOrderItemCmd implements ApiCommand {
    private Long orderId;
    private Long orderItemId;

    private Map<String, Object> parameters;
}
