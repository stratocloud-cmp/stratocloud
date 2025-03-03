package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AuditableRepository;
import com.stratocloud.permission.Permission;

public interface PermissionRepository extends AuditableRepository<Permission> {
    boolean existsByTargetAndAction(String target, String action);
}
