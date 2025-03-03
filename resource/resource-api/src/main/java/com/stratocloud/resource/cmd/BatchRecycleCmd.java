package com.stratocloud.resource.cmd;

import com.stratocloud.request.JobParameters;
import com.stratocloud.resource.cmd.recycle.RecycleCmd;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class BatchRecycleCmd implements JobParameters {
    private List<RecycleCmd> resources;

    @Override
    public void validate() {
        Assert.isNotEmpty(resources, "资源回收列表不能为空");

        resources.forEach(RecycleCmd::validate);
    }
}
