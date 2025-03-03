package com.stratocloud.resource.cmd.relationship;

import com.stratocloud.request.JobParameters;
import lombok.Data;

import java.util.List;

@Data
public class BatchDisconnectResourcesCmd implements JobParameters {
    private List<Long> relationshipIds;
}
