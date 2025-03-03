package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.role.Role;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoleJpaRepository extends TenantedJpaRepository<Role>, JpaSpecificationExecutor<Role> {
}
