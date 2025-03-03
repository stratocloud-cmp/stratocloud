package com.stratocloud.repository;

import com.stratocloud.job.Job;
import com.stratocloud.jpa.repository.jpa.ControllableJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobJpaRepository extends ControllableJpaRepository<Job>, JpaSpecificationExecutor<Job> {
}
