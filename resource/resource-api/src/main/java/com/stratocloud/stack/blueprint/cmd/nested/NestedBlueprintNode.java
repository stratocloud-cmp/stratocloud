package com.stratocloud.stack.blueprint.cmd.nested;

import lombok.Data;

import java.util.Map;

@Data
public class NestedBlueprintNode {
    private Long accountId;

    private String resourceTypeId;
    private String resourceTypeName;

    private String nodeKey;
    private String nodeName;

    private Boolean borrowed = false;

    private Long borrowedResourceId;

    private Map<String, Object> resourceProperties;
}
