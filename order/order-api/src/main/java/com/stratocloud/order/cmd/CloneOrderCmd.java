package com.stratocloud.order.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class CloneOrderCmd implements ApiCommand {
    private Long orderId;
}
