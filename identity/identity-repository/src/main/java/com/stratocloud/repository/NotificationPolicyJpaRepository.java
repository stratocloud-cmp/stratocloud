package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.notification.NotificationPolicy;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationPolicyJpaRepository
        extends TenantedJpaRepository<NotificationPolicy>, JpaSpecificationExecutor<NotificationPolicy> {
    boolean existsByPolicyKey(String policyKey);
}
