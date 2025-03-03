package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;
import com.stratocloud.permission.Permission;

public interface PermissionJpaRepository extends AuditableJpaRepository<Permission> {
    boolean existsByTargetAndAction(String target, String action);
}
