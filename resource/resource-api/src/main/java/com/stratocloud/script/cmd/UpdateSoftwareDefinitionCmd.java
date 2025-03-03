package com.stratocloud.script.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.resource.OsType;
import com.stratocloud.script.SoftwareType;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSoftwareDefinitionCmd implements ApiCommand {
    private Long softwareDefinitionId;

    private String name;
    private String description;

    private SoftwareType softwareType;

    private OsType osType;

    private boolean publicDefinition;

    private boolean visibleInTarget;

    private Integer servicePort;

    private List<NestedSoftwareAction> actions;

    private List<NestedSoftwareRequirement> requirements;
}
