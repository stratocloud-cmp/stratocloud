package com.stratocloud.notification.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class CreateNotificationWayCmd implements ApiCommand {
    private Long tenantId;

    private String providerId;

    private String name;

    private String description;

    private Map<String, Object> properties;
}
