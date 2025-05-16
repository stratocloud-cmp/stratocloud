package com.stratocloud.notification.query;

import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class NestedNotification extends NestedTenanted {
    private String eventId;
    private String eventType;
    private String eventTypeName;
    private StratoEventLevel eventLevel;
    private StratoEventSource eventSource;
    private String eventObjectType;
    private String eventObjectTypeName;
    private Long eventObjectId;
    private String eventObjectName;
    private String eventSummary;
    private LocalDateTime eventHappenedAt;
    private String eventProperties;

    private int sentCount;
    private LocalDateTime lastSentTime;

    private Long notificationPolicyId;
    private String notificationPolicyName;

    private Long notificationWayId;
    private String notificationWayName;

    private List<NestedNotificationReceiver> receivers;
}
