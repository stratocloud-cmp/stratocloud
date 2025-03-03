package com.stratocloud.script.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteScriptDefinitionsCmd implements ApiCommand {
    private List<Long> scriptDefinitionIds;
}
