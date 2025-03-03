package com.stratocloud.resource.query.metadata;

import com.stratocloud.resource.ResourceState;
import lombok.Data;

import java.util.Set;

@Data
public class NestedRelationshipSpec {
    private String relationshipTypeId;
    private String relationshipTypeName;
    private String requirementName;
    private String capabilityName;

    private String connectActionName;
    private String disconnectActionName;

    private String sourceResourceTypeId;
    private String targetResourceTypeId;

    private Boolean exclusiveRequirement;
    private Boolean essentialRequirement;
    private Boolean changeableEssential;
    private Boolean primaryCapability;
    private Boolean essentialPrimaryCapability;

    private Set<ResourceState> allowedSourceStates;
    private Set<ResourceState> allowedTargetStates;

    private Boolean visibleInTarget;
    private Boolean isolatedTargetContext;
}
