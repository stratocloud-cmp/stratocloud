package com.stratocloud.repository;

import com.stratocloud.job.JobDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface JobDefinitionJpaRepository
        extends JpaRepository<JobDefinition, String>, JpaSpecificationExecutor<JobDefinition> {
    Optional<JobDefinition> findByJobType(String jobType);

    Optional<JobDefinition> findByDefaultWorkflowId(Long workflowId);
}
