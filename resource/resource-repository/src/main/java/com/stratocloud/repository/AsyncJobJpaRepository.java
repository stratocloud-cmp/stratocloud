package com.stratocloud.repository;

import com.stratocloud.job.AsyncJob;
import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface AsyncJobJpaRepository extends ControllableJpaRepository<AsyncJob>, JpaSpecificationExecutor<AsyncJob> {
    List<AsyncJob> findByStartedAndEnded(boolean started, boolean ended);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AsyncJob> findByIdIs(Long jobId);
}
