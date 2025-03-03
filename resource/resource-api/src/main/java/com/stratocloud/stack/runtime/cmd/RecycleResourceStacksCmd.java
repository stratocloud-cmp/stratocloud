package com.stratocloud.stack.runtime.cmd;

import com.stratocloud.request.JobParameters;
import com.stratocloud.stack.runtime.cmd.nested.RecycleSingleStackCmd;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class RecycleResourceStacksCmd implements JobParameters {
    private List<RecycleSingleStackCmd> stacks;

    @Override
    public void validate() {
        Assert.isNotEmpty(stacks);
    }
}
