package com.stratocloud.resource.cmd.action;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class RunActionCmd implements ApiCommand {
    private Long resourceId;

    private String actionId;

    private Map<String, Object> parameters;
}
