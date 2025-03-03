package com.stratocloud.user.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class BatchAssignRoleToUserCmd implements ApiCommand {
    private List<NestedAssignRoleToUserCmd> assignList;
}
