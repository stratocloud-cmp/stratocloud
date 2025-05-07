package com.stratocloud.notification;

import com.stratocloud.jpa.entities.Tenanted;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPolicy extends Tenanted {
    @ManyToOne
    private NotificationEventType eventType;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationReceiverType receiverType;
    @Column
    private List<Long> presetUserIds;
    @Column
    private List<Long> presetUserGroupIds;
    @Column
    private List<Long> presetRoleIds;
    @Column(columnDefinition = "TEXT")
    private String receiverProvidingScript;


    @ManyToOne
    private NotificationWay notificationWay;
    @Column(columnDefinition = "TEXT")
    private String template;
    @Column(nullable = false)
    private int maxNotificationTimes = 1;
    @Column(nullable = false)
    private int notificationIntervalMinutes = 30;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications;

    public NotificationPolicy(NotificationEventType eventType,
                              String name,
                              String description,
                              NotificationReceiverType receiverType,
                              List<Long> presetUserIds,
                              List<Long> presetUserGroupIds,
                              List<Long> presetRoleIds,
                              String receiverProvidingScript,
                              NotificationWay notificationWay,
                              String template,
                              int maxNotificationTimes,
                              int notificationIntervalMinutes) {
        this.eventType = eventType;
        this.name = name;
        this.description = description;
        this.receiverType = receiverType;
        this.presetUserIds = presetUserIds;
        this.presetUserGroupIds = presetUserGroupIds;
        this.presetRoleIds = presetRoleIds;
        this.receiverProvidingScript = receiverProvidingScript;
        this.notificationWay = notificationWay;
        this.template = template;
        this.maxNotificationTimes = maxNotificationTimes;
        this.notificationIntervalMinutes = notificationIntervalMinutes;
    }

    public void update(String name,
                       String description,
                       NotificationReceiverType receiverType,
                       List<Long> presetUserIds,
                       List<Long> presetUserGroupIds,
                       List<Long> presetRoleIds,
                       String receiverProvidingScript,
                       String template,
                       int maxNotificationTimes,
                       int notificationIntervalMinutes){
        this.name = name;
        this.description = description;
        this.receiverType = receiverType;
        this.presetUserIds = presetUserIds;
        this.presetUserGroupIds = presetUserGroupIds;
        this.presetRoleIds = presetRoleIds;
        this.receiverProvidingScript = receiverProvidingScript;
        this.template = template;
        this.maxNotificationTimes = maxNotificationTimes;
        this.notificationIntervalMinutes = notificationIntervalMinutes;
    }
}
