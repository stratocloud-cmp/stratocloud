package com.stratocloud.resource.cmd;

import com.stratocloud.request.JobParameters;
import lombok.Data;

import java.util.List;

@Data
public class SynchronizeResourcesCmd implements JobParameters {
    private List<Long> resourceIds;
    private Boolean synchronizeAll;
}
