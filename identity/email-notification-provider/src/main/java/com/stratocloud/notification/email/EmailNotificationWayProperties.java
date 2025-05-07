package com.stratocloud.notification.email;

import com.stratocloud.notification.NotificationWayProperties;
import lombok.Data;

@Data
public class EmailNotificationWayProperties implements NotificationWayProperties {
    private String host;
    private Integer port;
    private String username;
    private String password;
}
