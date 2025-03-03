package com.stratocloud.stack.blueprint.cmd.nested;

import lombok.Data;

import java.util.Map;

@Data
public class NestedBlueprintRelationship {
    private String sourceNodeKey;
    private String sourceTypeId;

    private String targetNodeKey;
    private String targetTypeId;

    private String relationshipTypeId;
    private String connectActionName;
    private Map<String, Object> relationshipInput;
}
