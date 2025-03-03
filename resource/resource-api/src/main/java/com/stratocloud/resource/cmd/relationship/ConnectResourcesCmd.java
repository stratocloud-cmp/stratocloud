package com.stratocloud.resource.cmd.relationship;

import lombok.Data;

import java.util.Map;

@Data
public class ConnectResourcesCmd {
    private Long sourceResourceId;
    private Long targetResourceId;

    private String relationshipTypeId;
    private Map<String, Object> relationshipInputs;
}
