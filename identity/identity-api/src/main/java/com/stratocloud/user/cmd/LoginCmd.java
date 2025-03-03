package com.stratocloud.user.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class LoginCmd implements ApiCommand {
    private String loginName;
    private String password;
}
