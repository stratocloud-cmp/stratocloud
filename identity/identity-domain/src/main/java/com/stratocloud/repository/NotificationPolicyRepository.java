package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.notification.NotificationPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationPolicyRepository extends TenantedRepository<NotificationPolicy> {
    NotificationPolicy findNotificationPolicy(Long notificationPolicyId);

    Page<NotificationPolicy> page(String search, Pageable pageable);
}
