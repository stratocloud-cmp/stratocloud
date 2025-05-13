package com.stratocloud.notification;

import java.util.Arrays;
import java.util.Optional;

public enum NotificationReceiverType {
    PRESET_ROLES,
    PRESET_USERS,
    PRESET_USER_GROUPS,
    EVENT_OBJECT_OWNER,
    ORDER_HANDLERS;

    public static Optional<NotificationReceiverType> fromValue(String s){
        if(s == null)
            return Optional.empty();
        return Arrays.stream(values()).filter(v -> v.name().equals(s)).findAny();
    }
}
