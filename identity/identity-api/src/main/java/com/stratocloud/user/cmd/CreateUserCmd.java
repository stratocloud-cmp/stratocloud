package com.stratocloud.user.cmd;

import com.stratocloud.identity.BuiltInAuthTypes;
import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class CreateUserCmd implements ApiCommand {
    private Long tenantId;
    private String loginName;
    private String realName;
    private String emailAddress;
    private String phoneNumber;
    private String password;
    private Long iconId;
    private String description;
    private String authType = BuiltInAuthTypes.DEFAULT_AUTH_TYPE;
}
