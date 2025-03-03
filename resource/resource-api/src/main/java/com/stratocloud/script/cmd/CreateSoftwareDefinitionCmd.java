package com.stratocloud.script.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.resource.OsType;
import com.stratocloud.script.SoftwareType;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.List;

@Data
public class CreateSoftwareDefinitionCmd implements ApiCommand {
    private Long tenantId;
    private Long ownerId;

    private String definitionKey;

    private String name;
    private String description;

    private SoftwareType softwareType;

    private OsType osType;

    private boolean publicDefinition;

    private boolean visibleInTarget;

    private Integer servicePort;

    private List<NestedSoftwareAction> actions;

    private List<NestedSoftwareRequirement> requirements;

    @Override
    public void validate() {
        if(Utils.isNotEmpty(actions))
            actions.forEach(NestedSoftwareAction::validate);

        if(Utils.isNotEmpty(requirements))
            requirements.forEach(NestedSoftwareRequirement::validate);
    }
}
