package com.stratocloud.notification.cmd;

import com.stratocloud.notification.NotificationReceiverType;
import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class UpdateNotificationPolicyCmd implements ApiCommand {
    private Long notificationPolicyId;

    private String name;
    private String description;

    private NotificationReceiverType receiverType;
    private List<Long> presetUserIds;
    private List<Long> presetUserGroupIds;
    private List<Long> presetRoleIds;

    private String template;
    private int maxNotificationTimes = 1;
    private int notificationIntervalMinutes = 30;
}
