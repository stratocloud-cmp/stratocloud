package com.stratocloud.lock;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

@Slf4j
public class DefaultDistributedLockFailureHandler implements DistributedLockFailureHandler{
    @Override
    public Object handleLockFailure(ProceedingJoinPoint joinPoint, DistributedLock redisLock, String key) {
        log.warn("Distributed lock {} is being held by another thread/server.", key);
        return null;
    }
}
