package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.notification.Notification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationJpaRepository
        extends TenantedJpaRepository<Notification>, JpaSpecificationExecutor<Notification> {
}
