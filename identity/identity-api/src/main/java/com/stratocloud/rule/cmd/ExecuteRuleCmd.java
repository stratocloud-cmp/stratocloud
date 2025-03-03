package com.stratocloud.rule.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class ExecuteRuleCmd implements ApiCommand {
    private String ruleType;
    private Map<String, Object> parameters;
}
