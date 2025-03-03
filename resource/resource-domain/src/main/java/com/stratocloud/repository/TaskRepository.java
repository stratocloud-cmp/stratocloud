package com.stratocloud.repository;

import com.stratocloud.job.Task;
import com.stratocloud.job.TaskState;
import com.stratocloud.jpa.repository.AuditableRepository;

import java.util.List;

public interface TaskRepository extends AuditableRepository<Task> {
    List<Task> findByState(TaskState state);
}
