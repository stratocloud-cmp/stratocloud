package com.stratocloud.resource.cmd;

import com.stratocloud.request.JobParameters;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class BatchRestoreCmd implements JobParameters {
    private List<Long> resourceIds;

    @Override
    public void validate() {
        Assert.isNotEmpty(resourceIds, "资源还原列表不能为空");
    }
}
