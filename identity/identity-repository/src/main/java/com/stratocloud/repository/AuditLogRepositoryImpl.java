package com.stratocloud.repository;

import com.stratocloud.audit.AuditLog;
import com.stratocloud.audit.AuditLogLevel;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditLogRepositoryImpl extends AbstractTenantedRepository<AuditLog, AuditLogJpaRepository>
        implements AuditLogRepository {

    public AuditLogRepositoryImpl(AuditLogJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    public Page<AuditLog> page(String search,
                               List<AuditLogLevel> levels,
                               List<Long> tenantIds,
                               List<Long> userIds,
                               List<Integer> statusCodes,
                               Pageable pageable) {
        Specification<AuditLog> spec = getCallingTenantSpec();

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        if(Utils.isNotEmpty(levels))
            spec = spec.and(getLevelSpec(levels));

        if(Utils.isNotEmpty(tenantIds))
            spec = spec.and(getTenantSpec(tenantIds));

        if(Utils.isNotEmpty(userIds))
            spec = spec.and(getUserSpec(userIds));

        if(Utils.isNotEmpty(statusCodes))
            spec = spec.and(getStatusCodeSpec(statusCodes));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<AuditLog> getStatusCodeSpec(List<Integer> statusCodes) {
        return (root, query, criteriaBuilder)
                -> root.get("statusCode").in(statusCodes);
    }

    private Specification<AuditLog> getUserSpec(List<Long> userIds) {
        return (root, query, criteriaBuilder)
                -> root.get("userId").in(userIds);
    }

    private Specification<AuditLog> getLevelSpec(List<AuditLogLevel> levels) {
        return (root, query, criteriaBuilder)
                -> root.get("level").in(levels);
    }

    private Specification<AuditLog> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            String pattern = "%" + search + "%";

            Predicate p1 = criteriaBuilder.like(root.get("action"), pattern);
            Predicate p2 = criteriaBuilder.like(root.get("actionName"), pattern);
            Predicate p3 = criteriaBuilder.like(root.get("objectType"), pattern);
            Predicate p4 = criteriaBuilder.like(root.get("objectTypeName"), pattern);
            Predicate p5 = criteriaBuilder.like(root.get("objectNames"), pattern);
            Predicate p6 = criteriaBuilder.like(root.get("sourceIp"), pattern);
            Predicate p7 = criteriaBuilder.like(root.get("path"), pattern);

            return criteriaBuilder.or(p1,p2,p3,p4,p5,p6,p7);
        };
    }
}
