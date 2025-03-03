package com.stratocloud.resource.cmd.recycle;

import com.stratocloud.request.JobParameters;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.Map;

@Data
public class RecycleCmd implements JobParameters {
    private Long resourceId;
    private boolean recyclingCapabilities = true;
    private boolean executingDestruction = true;
    private Map<String, Object> destroyParameters;

    @Override
    public void validate() {
        Assert.isNotNull(resourceId);
    }
}
