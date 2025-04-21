package com.stratocloud.resource.task;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.job.Task;
import com.stratocloud.job.TaskHandler;
import com.stratocloud.job.TaskInputs;
import com.stratocloud.job.TaskType;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.repository.RelationshipRepository;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import com.stratocloud.resource.ResourceSynchronizer;
import com.stratocloud.resource.ResourceTaskLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
public class ConnectTaskHandler implements TaskHandler {

    private final RelationshipRepository relationshipRepository;

    private final ExternalAccountRepository accountRepository;

    private final ResourceSynchronizer resourceSynchronizer;

    private final ResourceTaskLockService resourceTaskLockService;

    public static final TaskType TASK_TYPE = new TaskType("CONNECT_RESOURCE", "资源建立关联任务");

    public ConnectTaskHandler(RelationshipRepository relationshipRepository,
                              ExternalAccountRepository accountRepository,
                              ResourceSynchronizer resourceSynchronizer,
                              ResourceTaskLockService resourceTaskLockService) {
        this.relationshipRepository = relationshipRepository;
        this.accountRepository = accountRepository;
        this.resourceSynchronizer = resourceSynchronizer;
        this.resourceTaskLockService = resourceTaskLockService;
    }

    @Override
    public TaskType getTaskType() {
        return TASK_TYPE;
    }

    @Override
    @Transactional(readOnly = true)
    public String getTaskName(Long entityId, TaskInputs taskInputs) {
        Relationship relationship = relationshipRepository.findRelationship(entityId);
        return relationship.getHandler().getConnectActionName();
    }



    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(Task task) {
        Relationship relationship = relationshipRepository.findRelationship(task.getEntityId());
        RelationshipHandler relationshipHandler = relationship.getHandler();

        resourceTaskLockService.acquireTaskLockOnTarget(relationship);

        ExternalAccount account = accountRepository.findExternalAccount(relationship.getSource().getAccountId());

        if(!relationshipHandler.isConnected(relationship, account))
            relationshipHandler.connect(relationship);

        relationship.onConnecting();
        relationshipRepository.save(relationship);
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkResult(Task task) {
        Relationship relationship = relationshipRepository.findRelationship(task.getEntityId());
        ExternalAccount account = accountRepository.findExternalAccount(relationship.getSource().getAccountId());
        RelationshipHandler relationshipHandler = relationship.getHandler();
        RelationshipActionResult result = relationshipHandler.checkConnectResult(account, relationship);

        switch (result.taskState()){
            case FINISHED -> {
                relationship.onConnected();
                task.onFinished();
                resourceTaskLockService.releaseTaskLockOnTarget(relationship);
            }
            case FAILED -> {
                relationship.onConnectionFailed(result.errorMessage());
                task.onFailed(result.errorMessage());
                resourceTaskLockService.releaseTaskLockOnTarget(relationship);
            }
            case STARTED -> log.warn("{} is connecting, checking later...", relationship.getEntityDescription());
        }

        relationshipRepository.save(relationship);
    }

    @Override
    @Transactional
    public void onDiscard(Task task) {
        Long relationshipId = task.getEntityId();

        Optional<Relationship> relationship = relationshipRepository.findById(relationshipId);
        relationship.ifPresent(relationshipRepository::delete);
    }

    @Override
    public synchronized void postHandleTaskFailure(Task task) {
        Long relationshipId = task.getEntityId();
        Optional<Relationship> relationship = relationshipRepository.findById(relationshipId);
        relationship.ifPresent(r -> resourceSynchronizer.synchronize(r.getSource().getId()));
    }
}
