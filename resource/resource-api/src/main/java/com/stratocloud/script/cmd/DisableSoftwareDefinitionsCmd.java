package com.stratocloud.script.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DisableSoftwareDefinitionsCmd implements ApiCommand {
    private List<Long> softwareDefinitionIds;
}
