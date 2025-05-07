package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.notification.NotificationWay;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationWayJpaRepository
        extends TenantedJpaRepository<NotificationWay>, JpaSpecificationExecutor<NotificationWay> {
}
