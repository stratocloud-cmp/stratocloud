package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;
import com.stratocloud.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TenantJpaRepository extends AuditableJpaRepository<Tenant>, JpaSpecificationExecutor<Tenant> {
    boolean existsByName(String name);
}
