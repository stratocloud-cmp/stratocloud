package com.stratocloud.event;

import java.time.LocalDateTime;

public record StratoEvent<P extends EventProperties>(String id,
                                                     StratoEventType type,
                                                     StratoEventLevel level,
                                                     StratoEventSource source,
                                                     StratoEventObject object,
                                                     String summary,
                                                     LocalDateTime eventHappenedAt,
                                                     P properties) {
}
