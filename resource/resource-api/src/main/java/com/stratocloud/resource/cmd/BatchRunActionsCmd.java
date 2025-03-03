package com.stratocloud.resource.cmd;

import com.stratocloud.request.BatchJobParameters;
import com.stratocloud.resource.cmd.action.RunActionCmd;
import com.stratocloud.utils.Assert;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchRunActionsCmd implements BatchJobParameters {

    private List<RunActionCmd> actions;

    @Override
    public void validate() {
        Assert.isNotEmpty(actions, "资源操作列表不能为空");
        actions.forEach(RunActionCmd::validate);
    }

    @Override
    public void merge(BatchJobParameters other) {
        if(!(other instanceof BatchRunActionsCmd otherCmd))
            return;

        if(Utils.isEmpty(otherCmd.getActions()))
            return;

        if(actions == null)
            actions = new ArrayList<>();

        actions.addAll(otherCmd.getActions());
    }
}
