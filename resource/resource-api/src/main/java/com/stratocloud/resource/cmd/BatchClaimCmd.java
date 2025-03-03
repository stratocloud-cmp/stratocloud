package com.stratocloud.resource.cmd;

import com.stratocloud.request.JobParameters;
import com.stratocloud.resource.cmd.ownership.ClaimCmd;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class BatchClaimCmd implements JobParameters {

    private List<ClaimCmd> claims;

    @Override
    public void validate() {
        Assert.isNotEmpty(claims, "资源认领列表不能为空");
    }
}
