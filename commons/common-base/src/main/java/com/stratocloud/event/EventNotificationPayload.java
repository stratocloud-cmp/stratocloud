package com.stratocloud.event;

import com.stratocloud.utils.JSON;

import java.time.LocalDateTime;
import java.util.Map;

public record EventNotificationPayload(String eventId,
                                       StratoEventType eventType,
                                       StratoEventLevel eventLevel,
                                       StratoEventSource eventSource,
                                       StratoEventObject eventObject,
                                       String summary,
                                       LocalDateTime eventHappenedAt,
                                       Map<String, Object> eventProperties) {

    public static EventNotificationPayload from(StratoEvent<?> event){
        return new EventNotificationPayload(
                event.id(),
                event.type(),
                event.level(),
                event.source(),
                event.object(),
                event.summary(),
                event.eventHappenedAt(),
                JSON.toMap(event.properties())
        );
    }
}
