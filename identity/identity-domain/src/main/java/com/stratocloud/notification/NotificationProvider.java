package com.stratocloud.notification;

import java.util.Map;

public interface NotificationProvider {

    String getId();

    String getName();

    void sendNotification(NotificationReceiver receiver);

    Class<? extends NotificationWayProperties> getPropertiesClass();

    void validateConnection(NotificationWay way);

    void eraseSensitiveInfo(Map<String, Object> properties);
}
