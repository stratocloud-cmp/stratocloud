package com.stratocloud.user.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class UnlockUsersCmd implements ApiCommand {
    private List<Long> userIds;
}
