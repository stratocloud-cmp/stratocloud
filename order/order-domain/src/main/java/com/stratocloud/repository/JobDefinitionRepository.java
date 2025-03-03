package com.stratocloud.repository;

import com.stratocloud.job.JobDefinition;
import com.stratocloud.jpa.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface JobDefinitionRepository extends Repository<JobDefinition, String> {
    JobDefinition findByJobType(String jobType);

    Optional<JobDefinition> findByDefaultWorkflowId(Long workflowId);

    Page<JobDefinition> page(List<String> jobTypes, String search, Pageable pageable);
}
