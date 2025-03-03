package com.stratocloud.repository;

import com.stratocloud.job.Task;
import com.stratocloud.job.TaskState;
import com.stratocloud.jpa.repository.jpa.AuditableJpaRepository;

import java.util.List;

public interface TaskJpaRepository extends AuditableJpaRepository<Task> {
    List<Task> findByState(TaskState state);
}
