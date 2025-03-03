package com.stratocloud.cart.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class CreateCartItemCmd implements ApiCommand {
    private String jobType;
    private Map<String, Object> jobParameters;
}
