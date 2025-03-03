package com.stratocloud.stack.runtime.cmd;

import com.stratocloud.request.JobParameters;
import com.stratocloud.stack.runtime.cmd.nested.TransferStackCmd;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class TransferStacksCmd implements JobParameters {

    private List<TransferStackCmd> transfers;

    @Override
    public void validate() {
        Assert.isNotEmpty(transfers, "资源栈移交列表不能为空");
    }
}
