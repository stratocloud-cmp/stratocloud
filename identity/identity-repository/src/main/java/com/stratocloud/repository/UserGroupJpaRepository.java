package com.stratocloud.repository;

import com.stratocloud.group.UserGroup;
import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserGroupJpaRepository extends TenantedJpaRepository<UserGroup>, JpaSpecificationExecutor<UserGroup> {
}
