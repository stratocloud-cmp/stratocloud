package com.stratocloud.resource.cmd;

import com.stratocloud.request.JobParameters;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class BatchDropCmd implements JobParameters {
    private List<Long> resourceIds;

    @Override
    public void validate() {
        Assert.isNotEmpty(resourceIds, "资源解除纳管列表不能为空");
    }
}
