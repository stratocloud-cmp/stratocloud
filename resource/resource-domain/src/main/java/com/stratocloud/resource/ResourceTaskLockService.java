package com.stratocloud.resource;

import com.stratocloud.cache.CacheLock;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.AutoRetryLaterException;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class ResourceTaskLockService {

    public static final String RESOURCE_TASK_LOCK_NAME_TEMPLATE = "resource-task-lock-%s";
    private final CacheService cacheService;

    public ResourceTaskLockService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    private void acquireTaskLock(Resource target, int maxLockSeconds){
        String lockName = getLockName(target);
        CacheLock lock = cacheService.getLock(lockName);

        if(lock.tryLock(maxLockSeconds))
            return;

        throw new AutoRetryLaterException(
                "Failed to acquire resource task lock, retrying later. LockName=%s.".formatted(lockName)
        );
    }

    private void releaseTaskLock(Resource target){
        String lockName = getLockName(target);
        CacheLock lock = cacheService.getLock(lockName);
        lock.unlock();
    }


    public void acquireTaskLockOnTarget(Relationship relationship) {
        RelationshipHandler handler = relationship.getHandler();

        if(handler.requireTargetResourceTaskLock())
            acquireTaskLock(relationship.getTarget(), handler.targetResourceTaskLockMaxSeconds());
    }

    public void releaseTaskLockOnTarget(Relationship relationship) {
        RelationshipHandler handler = relationship.getHandler();

        if(handler.requireTargetResourceTaskLock())
            releaseTaskLock(relationship.getTarget());
    }

    public void acquireTaskLock(Resource resource, ResourceAction action){
        ResourceActionHandler actionHandler = resource.getActionHandler(action.id());

        List<String> relTypeIds = actionHandler.getLockExclusiveTargetRelTypeIds();

        if(Utils.isEmpty(relTypeIds))
            return;

        for (String relTypeId : relTypeIds) {
            Optional<Resource> target = resource.getExclusiveTargetByType(relTypeId);

            if(target.isEmpty())
                continue;

            acquireTaskLock(target.get(), actionHandler.getLockExclusiveTargetMaxSeconds());
        }
    }

    public void releaseTaskLock(Resource resource, ResourceAction action){
        ResourceActionHandler actionHandler = resource.getActionHandler(action.id());

        List<String> relTypeIds = actionHandler.getLockExclusiveTargetRelTypeIds();

        if(Utils.isEmpty(relTypeIds))
            return;

        for (String relTypeId : relTypeIds) {
            Optional<Resource> target = resource.getExclusiveTargetByType(relTypeId);

            if(target.isEmpty()) {
                Optional<Relationship> disconnectedRel = resource.getExclusiveRequirements().stream().filter(
                        rel -> Objects.equals(relTypeId, rel.getType())
                ).findAny();
                disconnectedRel.ifPresent(rel -> releaseTaskLock(rel.getTarget()));
                continue;
            }

            releaseTaskLock(target.get());
        }


    }

    private static String getLockName(Resource resource) {
        return RESOURCE_TASK_LOCK_NAME_TEMPLATE.formatted(resource.getId());
    }
}
