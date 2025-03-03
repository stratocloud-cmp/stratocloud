package com.stratocloud.cart.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateCartItemCmd implements ApiCommand {
    private Long cartItemId;
    private Map<String, Object> jobParameters;
}
