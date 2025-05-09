package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationRepository extends TenantedRepository<Notification> {
    Page<Notification> page(String search, Pageable pageable);

    Notification findNotification(Long notificationId);

    List<Notification> findByPolicyIdAndSentCountLessThan(Long policyId, int sentCount);
}
