package com.stratocloud.user.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class ChangePasswordCmd implements ApiCommand {
    private Long userId;

    private String newPassword;
}
