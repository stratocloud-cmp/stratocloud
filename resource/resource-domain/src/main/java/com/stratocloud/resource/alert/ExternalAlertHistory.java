package com.stratocloud.resource.alert;

import com.stratocloud.event.StratoEventLevel;

import java.time.LocalDateTime;

public record ExternalAlertHistory(String id,
                                   StratoEventLevel level,
                                   AlertStatus alertStatus,
                                   String metricName,
                                   Long accountId,
                                   String resourceCategory,
                                   String externalResourceId,
                                   String message,
                                   LocalDateTime firstOccurredAt,
                                   LocalDateTime lastOccurredAt) {
}
