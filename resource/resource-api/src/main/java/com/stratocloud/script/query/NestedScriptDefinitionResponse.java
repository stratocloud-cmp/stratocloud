package com.stratocloud.script.query;

import com.stratocloud.request.query.NestedControllable;
import com.stratocloud.script.cmd.NestedRemoteScriptDef;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedScriptDefinitionResponse extends NestedControllable {
    private String definitionKey;

    private String name;
    private String description;

    private boolean publicDefinition;

    private boolean visibleInTarget;

    private boolean disabled;

    private NestedRemoteScriptDef remoteScriptDef;
}
