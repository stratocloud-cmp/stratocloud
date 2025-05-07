package com.stratocloud.notification.query;

import com.stratocloud.notification.NotificationReceiverType;
import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedNotificationPolicy extends NestedTenanted {
    private String eventType;
    private String eventTypeName;

    private String name;
    private String description;

    private NotificationReceiverType receiverType;
    private List<Long> presetUserIds;
    private List<Long> presetUserGroupIds;
    private List<Long> presetRoleIds;
    private String receiverProvidingScript;

    private Long notificationWayId;
    private String notificationWayName;

    private String template;
    private int maxNotificationTimes;
    private int notificationIntervalMinutes;
}
