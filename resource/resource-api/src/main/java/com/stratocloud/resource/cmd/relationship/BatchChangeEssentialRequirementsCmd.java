package com.stratocloud.resource.cmd.relationship;

import com.stratocloud.request.BatchJobParameters;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchChangeEssentialRequirementsCmd implements BatchJobParameters {
    private List<ChangeEssentialRequirementCmd> changes;

    @Override
    public void merge(BatchJobParameters other) {
        if(!(other instanceof BatchChangeEssentialRequirementsCmd otherCmd))
            return;

        if(Utils.isEmpty(otherCmd.getChanges()))
            return;

        if(changes == null)
            changes = new ArrayList<>();

        changes.addAll(otherCmd.getChanges());
    }
}
