package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import com.stratocloud.resource.Resource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ResourceJpaRepository extends ControllableJpaRepository<Resource>, JpaSpecificationExecutor<Resource> {
    boolean existsByName(String name);

    boolean existsByAccountIdAndTypeAndExternalId(Long accountId, String type, String externalId);

    Optional<Resource> findByAccountIdAndTypeAndExternalId(Long accountId, String type, String externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Resource> findByIdIs(Long resourceId);

    long countByName(String name);
}
