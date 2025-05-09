package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.notification.NotificationPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationPolicyRepository extends TenantedRepository<NotificationPolicy> {
    NotificationPolicy findNotificationPolicy(Long notificationPolicyId);

    Page<NotificationPolicy> page(String search, Pageable pageable);

    List<NotificationPolicy> findAllByEventType(String eventType);

    boolean existsByPolicyKey(String policyKey);
}
