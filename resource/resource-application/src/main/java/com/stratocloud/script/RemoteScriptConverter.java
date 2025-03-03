package com.stratocloud.script;

import com.stratocloud.script.cmd.NestedRemoteScriptDef;

public class RemoteScriptConverter {

    public static NestedRemoteScriptDef toNested(RemoteScriptDef remoteScriptDef){
        NestedRemoteScriptDef nestedRemoteScriptDef = new NestedRemoteScriptDef();

        nestedRemoteScriptDef.setRemoteScript(remoteScriptDef.getRemoteScript());
        remoteScriptDef.getCustomForm().ifPresent(
                nestedRemoteScriptDef::setCustomForm
        );

        return nestedRemoteScriptDef;
    }

    public static RemoteScriptDef fromNested(NestedRemoteScriptDef remoteScriptDef){
        return new RemoteScriptDef(
                remoteScriptDef.getRemoteScript(),
                remoteScriptDef.getCustomForm()
        );
    }

}
