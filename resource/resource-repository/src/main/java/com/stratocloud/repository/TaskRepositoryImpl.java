package com.stratocloud.repository;

import com.stratocloud.job.Task;
import com.stratocloud.job.TaskState;
import com.stratocloud.jpa.repository.AbstractAuditableRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class TaskRepositoryImpl extends AbstractAuditableRepository<Task, TaskJpaRepository> implements TaskRepository {

    public TaskRepositoryImpl(TaskJpaRepository jpaRepository) {
        super(jpaRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> findByState(TaskState state) {
        return jpaRepository.findByState(state);
    }
}
