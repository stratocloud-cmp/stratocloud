package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractAuditableRepository;
import com.stratocloud.notification.NotificationEventType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationEventTypeRepositoryImpl
        extends AbstractAuditableRepository<NotificationEventType, NotificationEventTypeJpaRepository>
        implements NotificationEventTypeRepository{
    public NotificationEventTypeRepositoryImpl(NotificationEventTypeJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationEventType findByEventType(String eventType) {
        return jpaRepository.findByEventType(eventType).orElseThrow(
                () -> new EntityNotFoundException("Event type not found: " + eventType)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEventType(String evenType) {
        return jpaRepository.existsByEventType(evenType);
    }
}
