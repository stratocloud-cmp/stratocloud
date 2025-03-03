package com.stratocloud.stack.runtime.cmd.nested;

import lombok.Data;

import java.util.Map;

@Data
public class NestedInternalRequirement {
    private String targetNodeKey;
    private String targetNodeName;
    private String targetResourceTypeId;
    private String relationshipTypeId;
    private String connectActionName;
    private Map<String, Object> relationshipInputs;
}
