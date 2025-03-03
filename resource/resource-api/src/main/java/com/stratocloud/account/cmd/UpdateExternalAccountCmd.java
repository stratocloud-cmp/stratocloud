package com.stratocloud.account.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateExternalAccountCmd implements ApiCommand {
    private Long externalAccountId;
    private String name;
    private Map<String, Object> properties;
    private String description;
}
