package com.stratocloud.stack.runtime.cmd;

import com.stratocloud.request.JobParameters;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class RestoreResourceStacksCmd implements JobParameters {
    private List<Long> stackIds;

    @Override
    public void validate() {
        Assert.isNotEmpty(stackIds);
    }
}
