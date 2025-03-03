package com.stratocloud.repository;

import com.stratocloud.job.AsyncJob;
import com.stratocloud.jpa.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AsyncJobRepository extends Repository<AsyncJob, Long> {
    Page<AsyncJob> page(List<Long> jobIds, Pageable pageable);

    List<AsyncJob> findByStartedAndEnded(boolean started, boolean ended);

    AsyncJob lockAsyncJob(Long jobId);
}
