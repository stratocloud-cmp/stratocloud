package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.job.JobDefinition;
import com.stratocloud.jpa.repository.AbstractRepository;
import com.stratocloud.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class JobDefinitionRepositoryImpl extends AbstractRepository<JobDefinition, String, JobDefinitionJpaRepository>
        implements JobDefinitionRepository {

    public JobDefinitionRepositoryImpl(JobDefinitionJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDefinition findByJobType(String jobType) {
        return jpaRepository.findByJobType(jobType).orElseThrow(
                () -> new EntityNotFoundException("Job definition not found: %s.".formatted(jobType))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobDefinition> findByDefaultWorkflowId(Long workflowId) {
        return jpaRepository.findByDefaultWorkflowId(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDefinition> page(List<String> jobTypes, String search, Pageable pageable) {
        Specification<JobDefinition> spec = getSpec();

        if(Utils.isNotEmpty(jobTypes))
            spec = spec.and(getTypeSpec(jobTypes));

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<JobDefinition> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("jobTypeName"), "%"+search+"%");
    }

    private Specification<JobDefinition> getTypeSpec(List<String> jobTypes) {
        return (root, query, criteriaBuilder) -> root.get("jobType").in(jobTypes);
    }
}
