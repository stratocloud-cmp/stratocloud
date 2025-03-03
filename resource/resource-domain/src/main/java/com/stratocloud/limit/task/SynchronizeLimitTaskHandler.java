package com.stratocloud.limit.task;

import com.stratocloud.job.Task;
import com.stratocloud.job.TaskHandler;
import com.stratocloud.job.TaskInputs;
import com.stratocloud.job.TaskType;
import com.stratocloud.limit.ResourceUsageLimitSynchronizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class SynchronizeLimitTaskHandler implements TaskHandler {

    public static final TaskType TASK_TYPE = new TaskType(
            "SYNCHRONIZE_RESOURCE_USAGE_LIMIT", "同步配额"
    );

    private final ResourceUsageLimitSynchronizer synchronizer;

    public SynchronizeLimitTaskHandler(ResourceUsageLimitSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    @Override
    public TaskType getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String getTaskName(Long entityId, TaskInputs taskInputs) {
        return "同步配额";
    }

    @Override
    @Transactional
    public void start(Task task) {
        synchronizer.synchronizeLimit(task.getEntityId());
    }

    @Override
    @Transactional
    public void checkResult(Task task) {
        task.onFinished();
    }

    @Override
    public void onDiscard(Task task) {
        log.warn("Synchronize task for {} discarded.", task.getEntityDescription());
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }
}
