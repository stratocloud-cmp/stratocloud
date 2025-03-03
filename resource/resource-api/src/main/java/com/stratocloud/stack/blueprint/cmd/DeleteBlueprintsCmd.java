package com.stratocloud.stack.blueprint.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class DeleteBlueprintsCmd implements ApiCommand {

    private List<Long> blueprintIds;

    @Override
    public void validate() {
        Assert.isNotEmpty(blueprintIds);
    }
}
