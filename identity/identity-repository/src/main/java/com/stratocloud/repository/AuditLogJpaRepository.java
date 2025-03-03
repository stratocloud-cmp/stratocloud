package com.stratocloud.repository;

import com.stratocloud.audit.AuditLog;
import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogJpaRepository extends TenantedJpaRepository<AuditLog>, JpaSpecificationExecutor<AuditLog> {
}
