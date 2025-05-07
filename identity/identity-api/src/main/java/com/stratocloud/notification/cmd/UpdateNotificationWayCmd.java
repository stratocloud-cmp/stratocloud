package com.stratocloud.notification.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateNotificationWayCmd implements ApiCommand {
    private Long notificationWayId;

    private String name;

    private String description;

    private Map<String, Object> properties;
}
