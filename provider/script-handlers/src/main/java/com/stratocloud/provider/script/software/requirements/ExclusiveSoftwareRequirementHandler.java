package com.stratocloud.provider.script.software.requirements;

import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.script.software.SoftwareHandler;
import com.stratocloud.script.SoftwareRequirement;

public class ExclusiveSoftwareRequirementHandler extends SoftwareRelationshipHandler implements ExclusiveRequirementHandler {

    public ExclusiveSoftwareRequirementHandler(SoftwareHandler sourceSoftwareHandler,
                                               SoftwareHandler targetSoftwareHandler,
                                               SoftwareRequirement softwareRequirement) {
        super(sourceSoftwareHandler, targetSoftwareHandler, softwareRequirement);
    }
}
