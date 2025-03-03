package com.stratocloud.role.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteRolesCmd implements ApiCommand {
    private List<Long> roleIds;
}
