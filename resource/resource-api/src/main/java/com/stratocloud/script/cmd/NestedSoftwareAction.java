package com.stratocloud.script.cmd;

import com.stratocloud.script.SoftwareActionType;
import com.stratocloud.utils.Assert;
import lombok.Data;

@Data
public class NestedSoftwareAction {
    private SoftwareActionType actionType;

    private String actionId;
    private String actionName;

    private NestedRemoteScriptDef remoteScriptDef;

    public void validate(){
        Assert.isNotNull(remoteScriptDef);
        Assert.isNotBlank(actionId);
        Assert.isNotNull(actionType);
        Assert.isNotBlank(actionName);
        remoteScriptDef.validate();
    }
}
