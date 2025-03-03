package com.stratocloud.repository;

import com.stratocloud.jpa.repository.AbstractControllableRepository;
import com.stratocloud.workflow.runtime.WorkflowInstance;
import org.springframework.stereotype.Repository;

@Repository
public class WorkflowInstanceRepositoryImpl
        extends AbstractControllableRepository<WorkflowInstance, WorkflowInstanceJpaRepository>
        implements WorkflowInstanceRepository {

    public WorkflowInstanceRepositoryImpl(WorkflowInstanceJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    public void validatePermission(WorkflowInstance entity) {

    }
}
