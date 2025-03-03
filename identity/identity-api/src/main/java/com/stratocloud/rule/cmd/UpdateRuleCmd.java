package com.stratocloud.rule.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRuleCmd implements ApiCommand {
    private Long ruleId;
    private String name;
    private String script;
}
