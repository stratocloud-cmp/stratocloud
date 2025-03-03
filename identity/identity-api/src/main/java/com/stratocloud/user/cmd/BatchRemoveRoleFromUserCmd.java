package com.stratocloud.user.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class BatchRemoveRoleFromUserCmd implements ApiCommand {
    private List<NestedRemoveRoleFromUserCmd> removeList;
}
