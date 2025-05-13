package com.stratocloud.notification;

import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventObject;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.utils.Utils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends Tenanted {
    @Column(nullable = false)
    private String eventId;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String eventTypeName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StratoEventLevel eventLevel;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StratoEventSource eventSource;
    @Column(nullable = false)
    private String eventObjectType;
    @Column(nullable = false)
    private String eventObjectTypeName;
    private Long eventObjectId;
    @Column(nullable = false)
    private String eventObjectName;
    @Column
    private Long eventObjectOwnerId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventSummary;
    @Column(nullable = false, updatable = false)
    private LocalDateTime eventHappenedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> eventProperties;
    @Column(nullable = false)
    private int sentCount = 0;
    @Column
    private LocalDateTime lastSentTime;

    @ManyToOne
    private NotificationPolicy policy;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "notification")
    private List<NotificationReceiver> receivers;

    public Notification(String eventId,
                        StratoEventLevel eventLevel,
                        StratoEventSource eventSource,
                        StratoEventObject eventObject,
                        String eventSummary,
                        LocalDateTime eventHappenedAt,
                        Map<String, Object> eventProperties,
                        NotificationPolicy policy) {
        this.eventId = eventId;
        this.eventType = policy.getEventType().getEventType();
        this.eventTypeName = policy.getEventType().getEventTypeName();
        this.eventLevel = eventLevel;
        this.eventSource = eventSource;
        this.eventObjectType = eventObject.objectType();
        this.eventObjectTypeName = eventObject.objectTypeName();
        this.eventObjectId = eventObject.objectId();
        this.eventObjectName = eventObject.objectName();
        this.eventObjectOwnerId = eventObject.ownerId();
        this.eventSummary = eventSummary;
        this.eventHappenedAt = eventHappenedAt;
        this.eventProperties = eventProperties;
        this.policy = policy;
    }

    public void send(List<Long> receiverUserIds) {
        sentCount++;
        lastSentTime = LocalDateTime.now();
        if(Utils.isEmpty(receiverUserIds))
            return;
        if(Utils.isNotEmpty(receivers)){
            for (NotificationReceiver receiver : receivers) {
                if(receiverUserIds.contains(receiver.getReceiverUserId()))
                    receiver.send();
            }
        }
    }

    public void sendToAll(){
        List<Long> userIds = new ArrayList<>();

        if(Utils.isNotEmpty(receivers))
            userIds.addAll(receivers.stream().map(NotificationReceiver::getReceiverUserId).toList());

        send(userIds);
    }
}
