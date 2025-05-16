package com.stratocloud.event;

import java.time.LocalDateTime;

public record ExternalResourceEvent(String id,
                                    StratoEventType type,
                                    StratoEventLevel level,
                                    StratoEventSource source,
                                    String resourceTypeId,
                                    Long accountId,
                                    String externalResourceId,
                                    String message,
                                    LocalDateTime happenedAt) {
}
