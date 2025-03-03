package com.stratocloud.resource.query.metadata;

import lombok.Data;

import java.util.List;

@Data
public class NestedResourceTypeCapability {
    private NestedRelationshipSpec relationshipSpec;
    private NestedResourceTypeSpec sourceSpec;
    private List<NestedResourceTypeRequirement> sourceRequirements;
    private List<NestedResourceTypeCapability> sourceCapabilities;
}
