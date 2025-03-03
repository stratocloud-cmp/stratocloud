package com.stratocloud.jpa.repository.jpa;

import com.stratocloud.jpa.entities.Tenanted;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface TenantedJpaRepository<E extends Tenanted> extends AuditableJpaRepository<E> {
    List<E> findByTenantIdIn(List<Long> tenantIds);

    boolean existsByTenantIdIn(List<Long> tenantIds);
}
