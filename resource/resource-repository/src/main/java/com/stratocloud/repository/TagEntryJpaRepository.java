package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.tag.ResourceTagEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TagEntryJpaRepository
        extends TenantedJpaRepository<ResourceTagEntry>, JpaSpecificationExecutor<ResourceTagEntry> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ResourceTagEntry> findByTagKey(String key);

    Optional<ResourceTagEntry> findByTagKeyIs(String key);
}
