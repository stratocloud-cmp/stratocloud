package com.stratocloud.resource.task;

import com.stratocloud.job.Task;
import com.stratocloud.job.TaskHandler;
import com.stratocloud.job.TaskInputs;
import com.stratocloud.job.TaskType;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceSyncState;
import com.stratocloud.resource.ResourceSynchronizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
public class SynchronizeResourceTaskHandler implements TaskHandler {

    public static final TaskType TASK_TYPE = new TaskType("SYNCHRONIZE_RESOURCE", "同步资源");

    private final ResourceRepository resourceRepository;


    private final ResourceSynchronizer synchronizer;

    public SynchronizeResourceTaskHandler(ResourceRepository resourceRepository,
                                          ResourceSynchronizer synchronizer) {
        this.resourceRepository = resourceRepository;
        this.synchronizer = synchronizer;
    }

    @Override
    public TaskType getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String getTaskName(Long entityId, TaskInputs taskInputs) {
        return "同步资源";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(Task task) {
        Resource resource = resourceRepository.findResource(task.getEntityId());
        synchronizer.synchronize(resource.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkResult(Task task) {
        Optional<Resource> resource = resourceRepository.findById(task.getEntityId());

        if(resource.isEmpty()) {
            String message = ("Resource %s is not found after SYNCHRONIZE_RESOURCE task started. " +
                    "Is it unmanaged already?").formatted(task.getEntityDescription());
            log.error(message);
            task.onFailed(message);
            return;
        }

        ResourceSyncState syncState = resource.get().getSyncState();

        switch (syncState){
            case NO_STATE -> {}
            case OK -> task.onFinished();
            case NOT_FOUND -> task.onFailed("External resource not found: %s.".formatted(task.getEntityDescription()));
            case CONNECTION_ERROR -> task.onFailed("Connection error.");
        }
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
