package com.stratocloud.resource.task;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.*;
import com.stratocloud.job.Task;
import com.stratocloud.job.TaskHandler;
import com.stratocloud.job.TaskInputs;
import com.stratocloud.job.TaskType;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.*;
import com.stratocloud.resource.event.ResourceActionEventProperties;
import com.stratocloud.resource.event.ResourceActionsEventHandler;
import com.stratocloud.resource.license.LicensedResourcesLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ResourceActionTaskHandler implements TaskHandler {

    public static final TaskType TASK_TYPE = new TaskType("RESOURCE_ACTION_TASK", "资源操作任务");

    private final ResourceRepository resourceRepository;

    private final ResourceSynchronizer resourceSynchronizer;

    private final ResourceTaskLockService lockService;

    private final LicensedResourcesLimiter licensedResourcesLimiter;

    private final ResourceActionsEventHandler eventHandler;

    public ResourceActionTaskHandler(ResourceRepository resourceRepository,
                                     ResourceSynchronizer resourceSynchronizer,
                                     ResourceTaskLockService lockService,
                                     LicensedResourcesLimiter licensedResourcesLimiter,
                                     ResourceActionsEventHandler eventHandler) {
        this.resourceRepository = resourceRepository;
        this.resourceSynchronizer = resourceSynchronizer;
        this.lockService = lockService;
        this.licensedResourcesLimiter = licensedResourcesLimiter;
        this.eventHandler = eventHandler;
    }

    @Override
    public TaskType getTaskType() {
        return TASK_TYPE;
    }

    @Override
    @Transactional(readOnly = true)
    public String getTaskName(Long entityId, TaskInputs taskInputs) {
        Resource resource = resourceRepository.findResource(entityId);

        ResourceHandler resourceHandler = resource.getResourceHandler();

        ResourceTaskInputs resourceTaskInputs = (ResourceTaskInputs) taskInputs;

        var actionHandler = resourceHandler.getActionHandler(resourceTaskInputs.resourceAction());

        if(actionHandler.isEmpty())
            return getTaskType().name();

        return actionHandler.get().getTaskName();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(Task task) {
        Resource resource = resourceRepository.lockResource(task.getEntityId());

        ResourceHandler resourceHandler = resource.getResourceHandler();

        ResourceTaskInputs resourceTaskInputs = (ResourceTaskInputs) task.getTaskInputs();
        ResourceAction action = resourceTaskInputs.resourceAction();
        Map<String, Object> parameters = resourceTaskInputs.parameters();

        if(ResourceActions.BUILD_RESOURCE.equals(action))
            licensedResourcesLimiter.validateLimitForCategory(resource.getCategory());

        lockService.acquireTaskLock(resource, action);

        resourceHandler.runAction(resource, action, parameters);

        resourceRepository.save(resource);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkResult(Task task) {
        Resource resource = resourceRepository.lockResource(task.getEntityId());

        ResourceHandler resourceHandler = resource.getResourceHandler();

        ResourceTaskInputs resourceTaskInputs = (ResourceTaskInputs) task.getTaskInputs();
        ResourceAction action = resourceTaskInputs.resourceAction();
        Map<String, Object> parameters = resourceTaskInputs.parameters();

        ResourceActionResult result = resourceHandler.checkActionResult(resource, action, parameters);

        switch (result.taskState()){
            case FINISHED -> {
                if(resource.getState() != ResourceState.DESTROYED)
                    resource.synchronize();

                resource.releasePreAllocatedUsagesByTaskId(task.getId());
                task.onFinished();
                lockService.releaseTaskLock(resource, action);
                eventHandler.handleEventQuietly(getEvent(resource, action, parameters, true));
            }
            case FAILED -> {
                task.onFailed(result.errorMessage());
                lockService.releaseTaskLock(resource, action);
                eventHandler.handleEventQuietly(getEvent(resource, action, parameters, false));
            }
            case STARTED -> log.warn("Task {} is running, checking later...", task.getId());
        }

        resourceRepository.save(resource);
    }

    private StratoEvent<ResourceActionEventProperties> getEvent(Resource resource,
                                                                ResourceAction action,
                                                                Map<String, Object> actionInputs,
                                                                boolean success) {
        ExternalAccount account = resource.getResourceHandler().getAccountRepository().findExternalAccount(
                resource.getAccountId()
        );
        ResourceActionEventProperties eventProperties = ResourceActionEventProperties.create(
                resource, account, action, actionInputs
        );

        StratoEventType eventType = eventHandler.getEventType(resource.getActionHandler(action.id()), success);
        return new StratoEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                success ? StratoEventLevel.INFO : StratoEventLevel.WARNING,
                StratoEventSource.STRATO_ACTION,
                eventHandler.getEventObject(resource),
                eventType.name() + ": " + resource.getName(),
                LocalDateTime.now(),
                eventProperties
        );
    }

    @Override
    @Transactional
    public void onDiscard(Task task) {
        ResourceTaskInputs taskInputs = (ResourceTaskInputs) task.getTaskInputs();
        Optional<Resource> resource = resourceRepository.findById(task.getEntityId());
        if(ResourceActions.BUILD_RESOURCE.equals(taskInputs.resourceAction())) {
            resource.ifPresent(resourceRepository::delete);
        }else {
            resource.ifPresent(r->{
                r.releasePreAllocatedUsagesByTaskId(task.getId());
                resourceRepository.save(r);
            });
        }
    }

    @Override
    public void postHandleTaskFailure(Task task) {
        resourceSynchronizer.synchronize(task.getEntityId());
    }
}
