package com.stratocloud.user.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class UpdateUserCmd implements ApiCommand {
    private Long userId;

    private String realName;
    private String emailAddress;
    private String phoneNumber;

    private String description;
}
