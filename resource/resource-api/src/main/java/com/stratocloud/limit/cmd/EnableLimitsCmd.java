package com.stratocloud.limit.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class EnableLimitsCmd implements ApiCommand {
    private List<Long> limitIds;

    @Override
    public void validate() {
        Assert.isNotEmpty(limitIds, "Limit ids cannot be empty");
    }
}
