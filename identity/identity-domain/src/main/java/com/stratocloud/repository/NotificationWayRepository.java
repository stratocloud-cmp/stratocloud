package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.notification.NotificationWay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationWayRepository extends TenantedRepository<NotificationWay> {
    NotificationWay findNotificationWay(Long notificationWayId);

    Page<NotificationWay> page(String search, Pageable pageable);
}
