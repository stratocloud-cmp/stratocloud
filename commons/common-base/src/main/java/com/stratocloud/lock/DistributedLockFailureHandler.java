package com.stratocloud.lock;

import org.aspectj.lang.ProceedingJoinPoint;

public interface DistributedLockFailureHandler {
    Object handleLockFailure(ProceedingJoinPoint joinPoint, DistributedLock redisLock, String key);
}
