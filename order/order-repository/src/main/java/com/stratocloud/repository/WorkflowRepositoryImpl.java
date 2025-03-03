package com.stratocloud.repository;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.jpa.repository.AbstractTenantedRepository;
import com.stratocloud.utils.Utils;
import com.stratocloud.workflow.Workflow;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class WorkflowRepositoryImpl extends AbstractTenantedRepository<Workflow, WorkflowJpaRepository>
        implements WorkflowRepository {

    public WorkflowRepositoryImpl(WorkflowJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Workflow findWorkflow(Long workflowId) {
        return jpaRepository.findById(workflowId).orElseThrow(
                () -> new EntityNotFoundException("Workflow not found.")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Workflow> page(List<Long> workflowIds, Boolean isReplica, String search, Pageable pageable) {
        Specification<Workflow> spec = getWorkflowSpecification(workflowIds, isReplica, search);


        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<Workflow> getWorkflowSpecification(List<Long> workflowIds, Boolean isReplica, String search) {
        Specification<Workflow> spec = getCallingTenantSpec();

        if(Utils.isNotEmpty(workflowIds))
            spec = spec.and(getIdSpec(workflowIds));

        if(isReplica != null)
            spec = spec.and(getIsReplicaSpec(isReplica));

        if(Utils.isNotBlank(search))
            spec = spec.and(getSearchSpec(search));
        return spec;
    }

    private Specification<Workflow> getSearchSpec(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("name"), "%"+search+"%");
    }

    private Specification<Workflow> getIsReplicaSpec(Boolean isReplica) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isReplica"), isReplica);
    }
}
