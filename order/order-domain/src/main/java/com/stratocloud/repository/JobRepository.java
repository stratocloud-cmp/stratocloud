package com.stratocloud.repository;

import com.stratocloud.job.Job;
import com.stratocloud.job.JobFilters;
import com.stratocloud.jpa.repository.ControllableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobRepository extends ControllableRepository<Job> {
    Page<Job> page(JobFilters jobFilters, Pageable pageable);
}
