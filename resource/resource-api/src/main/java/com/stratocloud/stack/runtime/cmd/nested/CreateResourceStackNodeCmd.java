package com.stratocloud.stack.runtime.cmd.nested;

import com.stratocloud.resource.cmd.create.CreateResourcesCmd;
import lombok.Data;

import java.util.List;

@Data
public class CreateResourceStackNodeCmd {
    private String nodeKey;
    private String nodeName;

    private CreateResourcesCmd resourceCmd;

    private List<NestedInternalRequirement> internalRequirements;
}
