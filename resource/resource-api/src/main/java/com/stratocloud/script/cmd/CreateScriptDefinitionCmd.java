package com.stratocloud.script.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.utils.Assert;
import lombok.Data;

@Data
public class CreateScriptDefinitionCmd implements ApiCommand {
    private Long tenantId;
    private Long ownerId;

    private String definitionKey;

    private String name;
    private String description;

    private boolean publicDefinition;

    private boolean visibleInTarget;

    private NestedRemoteScriptDef remoteScriptDef;

    @Override
    public void validate() {
        Assert.isNotNull(remoteScriptDef);
        remoteScriptDef.validate();
    }
}
