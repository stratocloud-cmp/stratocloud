package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface ResourceJpaRepository extends ControllableJpaRepository<Resource>, JpaSpecificationExecutor<Resource> {
    boolean existsByName(String name);

    boolean existsByAccountIdAndTypeAndExternalIdAndStateIn(Long accountId, String type, String externalId, List<ResourceState> states);

    Optional<Resource> findByAccountIdAndTypeAndExternalIdAndStateIn(Long accountId, String type, String externalId, List<ResourceState> states);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Resource> findByIdIs(Long resourceId);

    long countByName(String name);
}
