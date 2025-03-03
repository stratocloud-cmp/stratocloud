package com.stratocloud.provider.relationship;

import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;

import java.util.Map;

public interface ChangeableEssentialHandler extends EssentialRequirementHandler {
    default ResourceCost getChangeCost(Resource source, Resource newTarget, Map<String, Object> relationshipInputs){
        return ResourceCost.ZERO;
    }

}
