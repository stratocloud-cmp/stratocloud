package com.stratocloud.repository;

import com.stratocloud.jpa.repository.TenantedRepository;
import com.stratocloud.workflow.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WorkflowRepository extends TenantedRepository<Workflow> {
    Workflow findWorkflow(Long workflowId);

    Page<Workflow> page(List<Long> workflowIds, Boolean isReplica, String search, Pageable pageable);
}
