package com.stratocloud.repository;

import com.stratocloud.audit.AuditLog;
import com.stratocloud.audit.AuditLogLevel;
import com.stratocloud.jpa.repository.TenantedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuditLogRepository extends TenantedRepository<AuditLog> {

    Page<AuditLog> page(String search,
                        List<AuditLogLevel> levels,
                        List<Long> tenantIds,
                        List<Long> userIds,
                        List<Integer> statusCodes,
                        Pageable pageable);

}
