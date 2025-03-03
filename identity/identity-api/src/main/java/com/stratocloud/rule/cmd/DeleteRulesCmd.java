package com.stratocloud.rule.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteRulesCmd implements ApiCommand {
    private List<Long> ruleIds;
}
