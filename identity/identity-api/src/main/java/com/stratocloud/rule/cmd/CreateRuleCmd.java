package com.stratocloud.rule.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRuleCmd implements ApiCommand {
    private Long tenantId;
    private String type;
    private String name;
    private String script;
}
