package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.limit.ResourceUsageLimit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceUsageLimitRepository extends TenantedRepository<ResourceUsageLimit> {
    Page<ResourceUsageLimit> page(List<Long> tenantIds, String search, Pageable pageable);

    ResourceUsageLimit findLimit(Long limitId);
}
