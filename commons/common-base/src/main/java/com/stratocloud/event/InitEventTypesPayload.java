package com.stratocloud.event;

import java.util.List;
import java.util.Map;

public record InitEventTypesPayload(List<StratoEventType> eventTypes,
                                    Map<String, Object> eventPropertiesExample,
                                    List<BuiltInNotificationPolicy> builtInNotificationPolicies) {
}
