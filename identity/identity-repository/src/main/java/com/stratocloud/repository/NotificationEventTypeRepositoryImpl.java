package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractAuditableRepository;
import com.stratocloud.notification.NotificationEventType;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationEventTypeRepositoryImpl
        extends AbstractAuditableRepository<NotificationEventType, NotificationEventTypeJpaRepository>
        implements NotificationEventTypeRepository{
    public NotificationEventTypeRepositoryImpl(NotificationEventTypeJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    public NotificationEventType findByEventType(String eventType) {
        return jpaRepository.findByEventType(eventType).orElseThrow(
                () -> new EntityNotFoundException("Event type not found: " + eventType)
        );
    }
}
