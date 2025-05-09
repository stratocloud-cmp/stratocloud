package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;
import com.stratocloud.notification.NotificationEventType;

import java.util.Optional;

public interface NotificationEventTypeJpaRepository extends AuditableJpaRepository<NotificationEventType> {
    Optional<NotificationEventType> findByEventType(String eventType);

    boolean existsByEventType(String evenType);
}
