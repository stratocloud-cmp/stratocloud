package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AuditableRepository;
import com.stratocloud.tenant.Tenant;
import com.stratocloud.tenant.TenantFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TenantRepository extends AuditableRepository<Tenant> {
    List<Tenant> findInheritedTenants(Long tenantId);

    List<Tenant> findSubTenants(Long tenantId);

    Tenant findTenant(Long tenantId);

    Page<Tenant> page(TenantFilters tenantFilters, Pageable pageable);

    List<Tenant> findVisibleRoots(boolean includeInherited);

    boolean existsByName(String name);
}
