package com.stratocloud.resource.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class RunReadActionsCmd implements ApiCommand {
    private List<Long> resourceIds;
    private String actionId;

    @Override
    public void validate() {
        Assert.isNotBlank(actionId, "Action id required.");
        Assert.isNotEmpty(resourceIds, "Resource ids required.");
    }
}
