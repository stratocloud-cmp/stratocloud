package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AuditableRepository;
import com.stratocloud.notification.NotificationEventType;

public interface NotificationEventTypeRepository extends AuditableRepository<NotificationEventType> {
    NotificationEventType findByEventType(String eventType);

    boolean existsByEventType(String evenType);
}
