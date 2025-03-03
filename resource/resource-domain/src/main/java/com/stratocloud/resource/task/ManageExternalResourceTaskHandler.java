package com.stratocloud.resource.task;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.identity.BuiltInIds;
import com.stratocloud.job.Task;
import com.stratocloud.job.TaskHandler;
import com.stratocloud.job.TaskInputs;
import com.stratocloud.job.TaskType;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.*;
import com.stratocloud.resource.license.LicensedResourcesLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
public class ManageExternalResourceTaskHandler implements TaskHandler {

    public static final TaskType TASK_TYPE = new TaskType("MANAGE_EXTERNAL_RESOURCE", "纳管资源");

    private final ExternalAccountRepository accountRepository;

    private final ResourceRepository resourceRepository;


    private final ResourceSynchronizer synchronizer;

    private final LicensedResourcesLimiter licensedResourcesLimiter;

    public ManageExternalResourceTaskHandler(ExternalAccountRepository accountRepository,
                                             ResourceRepository resourceRepository,
                                             ResourceSynchronizer synchronizer,
                                             LicensedResourcesLimiter licensedResourcesLimiter) {
        this.accountRepository = accountRepository;
        this.resourceRepository = resourceRepository;
        this.synchronizer = synchronizer;
        this.licensedResourcesLimiter = licensedResourcesLimiter;
    }

    @Override
    public TaskType getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public String getTaskName(Long entityId, TaskInputs taskInputs) {
        return "纳管资源";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(Task task) {
        var taskInputs = (ManageExternalResourceTaskInputs) task.getTaskInputs();
        ExternalResource externalResource = taskInputs.externalResource();

        manageExternalResource(externalResource);
    }

    private void manageExternalResource(ExternalResource externalResource) {
        log.info("Managing external resource {}.", externalResource.name());

        ExternalAccount account = accountRepository.findExternalAccount(externalResource.accountId());

        Resource resource;
        Optional<Resource> optionalResource = resourceRepository.findByExternalResource(externalResource);

        if(optionalResource.isPresent()){
            log.info("Resource {} is already managed.", externalResource.name());
            resource = optionalResource.get();
        }else {
            log.info("Resource {} is never managed.", externalResource.name());
            resource = Resource.createFromExternalResource(
                    account.getTenantId(), BuiltInIds.SYSTEM_USER_ID, externalResource
            );

            licensedResourcesLimiter.validateLimitForCategory(resource.getCategory());
        }

        resource = resourceRepository.saveWithSystemSession(resource);

        synchronizer.synchronize(resource.getId());

        log.info("Resource {} has been managed successfully.", resource.getName());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkResult(Task task) {
        var taskInputs = (ManageExternalResourceTaskInputs) task.getTaskInputs();
        ExternalResource externalResource = taskInputs.externalResource();


        Optional<Resource> resource = resourceRepository.findByExternalResource(externalResource);

        if(resource.isEmpty()) {
            String message = ("Resource %s is not found after MANAGE_EXTERNAL_RESOURCE task started. " +
                    "Is it unmanaged already?").formatted(externalResource.name());
            log.error(message);
            task.onFailed(message);
            return;
        }

        ResourceSyncState syncState = resource.get().getSyncState();

        switch (syncState){
            case NO_STATE -> {}
            case OK -> task.onFinished();
            case NOT_FOUND -> task.onFailed("External resource not found: %s.".formatted(externalResource.name()));
            case CONNECTION_ERROR -> task.onFailed("Connection error.");
        }
    }

    @Override
    public void onDiscard(Task task) {
        log.warn("Manage task for {} discarded.", task.getEntityDescription());
    }
}
