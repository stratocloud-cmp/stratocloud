package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class AsyncJobRepositoryImpl extends AbstractControllableRepository<AsyncJob, AsyncJobJpaRepository>
        implements AsyncJobRepository {
    public AsyncJobRepositoryImpl(AsyncJobJpaRepository jpaRepository) {
        super(jpaRepository);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<AsyncJob> page(List<Long> jobIds, Pageable pageable) {
        Specification<AsyncJob> spec = getCallingTenantSpec();

        spec = spec.and(getCallingOwnerSpec());

        if(Utils.isNotEmpty(jobIds))
            spec = spec.and(getIdSpec(jobIds));

        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsyncJob> findByStartedAndEnded(boolean started, boolean ended) {
        return jpaRepository.findByStartedAndEnded(started, ended);
    }

    @Override
    @Transactional(readOnly = true)
    public AsyncJob lockAsyncJob(Long jobId) {
        return jpaRepository.findByIdIs(jobId).orElseThrow(
                () -> new EntityNotFoundException("Async job not found by id: %s.".formatted(jobId))
        );
    }
}
