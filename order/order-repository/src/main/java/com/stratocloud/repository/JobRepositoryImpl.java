package com.stratocloud.repository;

import com.stratocloud.job.Job;
import com.stratocloud.job.JobDefinition;
import com.stratocloud.job.JobFilters;
import com.stratocloud.job.JobStatus;
import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.utils.Utils;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JobRepositoryImpl extends AbstractControllableRepository<Job, JobJpaRepository> implements JobRepository {

    public JobRepositoryImpl(JobJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    public Page<Job> page(JobFilters jobFilters, Pageable pageable) {
        Specification<Job> spec = getJobSpecification(jobFilters);

        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Job> getJobSpecification(JobFilters jobFilters) {
        Specification<Job> spec = getCallingTenantSpec();

        spec = spec.and(getCallingOwnerSpec());

        if(Utils.isNotEmpty(jobFilters.jobIds()))
            spec = spec.and(getIdSpec(jobFilters.jobIds()));

        if(Utils.isNotEmpty(jobFilters.tenantIds()))
            spec = spec.and(getTenantSpec(jobFilters.tenantIds()));

        if(Utils.isNotEmpty(jobFilters.ownerIds()))
            spec = spec.and(getOwnerSpec(jobFilters.ownerIds()));

        if(Utils.isNotEmpty(jobFilters.jobStatuses()))
            spec = spec.and(getStatusSpec(jobFilters.jobStatuses()));

        if(Utils.isNotBlank(jobFilters.search()))
            spec = spec.and(getSearchSpec(jobFilters.search()));

        return spec;
    }

    private Specification<Job> getStatusSpec(List<JobStatus> jobStatuses) {
        return (root, query, criteriaBuilder) -> root.get("status").in(jobStatuses);
    }

    private Specification<Job> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> {
            Join<JobDefinition, Job> join = root.join("jobDefinition");
            Predicate p1 = criteriaBuilder.like(join.get("jobType"), "%" + search + "%");
            Predicate p2 = criteriaBuilder.like(join.get("jobTypeName"), "%" + search + "%");
            return criteriaBuilder.or(p1, p2);
        };
    }
}
