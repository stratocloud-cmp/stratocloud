package com.stratocloud.script.query;

import com.stratocloud.request.query.NestedControllable;
import com.stratocloud.resource.OsType;
import com.stratocloud.script.SoftwareType;
import com.stratocloud.script.cmd.NestedSoftwareAction;
import com.stratocloud.script.cmd.NestedSoftwareRequirement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NestedSoftwareDefinitionResponse extends NestedControllable {
    private String definitionKey;

    private String name;
    private String description;

    private SoftwareType softwareType;

    private OsType osType;

    private boolean publicDefinition;

    private boolean visibleInTarget;

    private boolean disabled;

    private Integer servicePort;

    private List<NestedSoftwareAction> actions = new ArrayList<>();

    private List<NestedSoftwareRequirement> requirements = new ArrayList<>();
}
