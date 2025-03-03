package com.stratocloud.resource.query.metadata;

import lombok.Data;

import java.util.List;

@Data
public class NestedResourceType {
    private NestedResourceTypeSpec spec;

    private List<NestedResourceTypeRequirement> requirements;
    private List<NestedResourceTypeCapability> capabilities;
}
