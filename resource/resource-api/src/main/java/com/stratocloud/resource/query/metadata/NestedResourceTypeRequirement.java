package com.stratocloud.resource.query.metadata;

import lombok.Data;

import java.util.List;

@Data
public class NestedResourceTypeRequirement {
    private NestedRelationshipSpec relationshipSpec;
    private NestedResourceTypeSpec targetSpec;
    private List<NestedResourceTypeRequirement> targetRequirements;
}
