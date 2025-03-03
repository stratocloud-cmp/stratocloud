package com.stratocloud.resource.cmd.create;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NestedNewCapability {
    private NestedNewResource resource;

    private String relationshipTypeId;
    private Map<String, Object> relationshipProperties;


    private List<NestedNewRequirement> requirements;

    private List<NestedNewCapability> capabilities;
}
