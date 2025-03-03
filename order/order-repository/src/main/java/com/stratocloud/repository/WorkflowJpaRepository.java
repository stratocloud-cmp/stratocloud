package com.stratocloud.repository;

import com.stratocloud.jpa.repository.jpa.TenantedJpaRepository;
import com.stratocloud.workflow.Workflow;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WorkflowJpaRepository extends TenantedJpaRepository<Workflow>, JpaSpecificationExecutor<Workflow> {
}
