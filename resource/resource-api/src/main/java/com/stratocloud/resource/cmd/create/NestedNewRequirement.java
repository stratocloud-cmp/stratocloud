package com.stratocloud.resource.cmd.create;

import lombok.Data;

import java.util.Map;

@Data
public class NestedNewRequirement {
    private Long targetResourceId;

    private String relationshipTypeId;
    private Map<String, Object> relationshipInputs;
}
