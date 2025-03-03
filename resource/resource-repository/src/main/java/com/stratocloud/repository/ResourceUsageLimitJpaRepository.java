package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.limit.ResourceUsageLimit;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ResourceUsageLimitJpaRepository
        extends TenantedJpaRepository<ResourceUsageLimit>, JpaSpecificationExecutor<ResourceUsageLimit> {
}
