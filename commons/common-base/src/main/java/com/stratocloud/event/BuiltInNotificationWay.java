package com.stratocloud.event;

import java.util.Map;

public record BuiltInNotificationWay(String providerId,
                                     String name,
                                     String description,
                                     Map<String, Object> properties) {
}
