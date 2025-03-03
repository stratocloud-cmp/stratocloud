package com.stratocloud.group.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteUserGroupsCmd implements ApiCommand {
    private List<Long> userGroupIds;
}
