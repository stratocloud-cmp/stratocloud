package com.stratocloud.resource.cmd;

import com.stratocloud.request.JobParameters;
import com.stratocloud.resource.cmd.ownership.TransferCmd;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class BatchTransferCmd implements JobParameters {

    private List<TransferCmd> transfers;

    @Override
    public void validate() {
        Assert.isNotEmpty(transfers, "资源移交列表不能为空");
    }
}
