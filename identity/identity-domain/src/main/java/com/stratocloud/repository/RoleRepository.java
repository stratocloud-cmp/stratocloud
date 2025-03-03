package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.role.Role;
import com.stratocloud.role.RoleFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleRepository extends TenantedRepository<Role> {
    Role findRole(Long roleId);

    Page<Role> page(RoleFilters filters, Pageable pageable);
}
