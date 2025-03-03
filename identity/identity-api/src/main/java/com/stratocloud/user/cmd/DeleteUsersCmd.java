package com.stratocloud.user.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class DeleteUsersCmd implements ApiCommand {
    private List<Long> userIds;

    @Override
    public void validate() {
        Assert.isNotEmpty(userIds, "未指定用户");
    }
}
