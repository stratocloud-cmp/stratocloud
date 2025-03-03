package com.stratocloud.account.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class CreateExternalAccountCmd implements ApiCommand {
    private String providerId;
    private String name;
    private Map<String, Object> properties;
    private String description;
}
