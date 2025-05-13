package com.stratocloud.event;

import java.util.List;

public record BuiltInNotificationPolicy(StratoEventType eventType,
                                        String policyKey,
                                        String name,
                                        String description,
                                        String receiverType,
                                        List<Long> presetUserIds,
                                        List<Long> presetUserGroupIds,
                                        List<Long> presetRoleIds,
                                        BuiltInNotificationWay notificationWay,
                                        String template,
                                        int maxNotificationTimes,
                                        int notificationIntervalMinutes) {
}
