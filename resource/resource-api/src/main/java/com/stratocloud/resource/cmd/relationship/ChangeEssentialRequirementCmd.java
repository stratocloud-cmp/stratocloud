package com.stratocloud.resource.cmd.relationship;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class ChangeEssentialRequirementCmd implements ApiCommand {
    private Long sourceId;
    private Long newTargetId;
    private String relationshipTypeId;
    private Map<String, Object> relationshipInputs;
}
