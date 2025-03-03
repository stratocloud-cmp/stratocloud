package com.stratocloud.script.cmd;

import com.stratocloud.utils.Assert;
import lombok.Data;

@Data
public class NestedSoftwareRequirement {

    private Long targetSoftwareDefinitionId;

    private String requirementKey;
    private String requirementName;
    private String capabilityName;

    private boolean exclusive;

    private NestedRemoteScriptDef connectScriptDef;

    private NestedRemoteScriptDef disconnectScriptDef;

    private NestedRemoteScriptDef checkConnectionScriptDef;

    public void validate(){
        Assert.isNotNull(targetSoftwareDefinitionId);
        Assert.isNotBlank(requirementKey);
        Assert.isNotBlank(requirementName);
        Assert.isNotBlank(capabilityName);
        Assert.isNotNull(connectScriptDef);
        Assert.isNotNull(disconnectScriptDef);
        Assert.isNotNull(checkConnectionScriptDef);
        connectScriptDef.validate();
        disconnectScriptDef.validate();
        checkConnectionScriptDef.validate();
    }
}
