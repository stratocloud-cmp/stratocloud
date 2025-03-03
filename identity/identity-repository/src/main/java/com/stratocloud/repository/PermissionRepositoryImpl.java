package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AbstractAuditableRepository;
import com.stratocloud.permission.Permission;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PermissionRepositoryImpl extends AbstractAuditableRepository<Permission, PermissionJpaRepository>
        implements PermissionRepository {

    public PermissionRepositoryImpl(PermissionJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTargetAndAction(String target, String action) {
        return jpaRepository.existsByTargetAndAction(target, action);
    }
}
