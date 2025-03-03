package com.stratocloud.lock;

import com.stratocloud.cache.CacheLock;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.Assert;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Slf4j
@Component
public class DistributedLockAspect {

    private final CacheService cacheService;

    public DistributedLockAspect(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Around("@annotation(distributedLock)")
    public Object aroundLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) {
        int maxWaitTime = distributedLock.maxWaitTime();
        int maxLockSeconds = distributedLock.maxLockSeconds();

        Assert.isPositive(maxWaitTime, maxLockSeconds);

        Object[] arguments = joinPoint.getArgs();
        DistributedLockKeyGenerator keyGenerator = getKeyGenerator(distributedLock);

        final String key = keyGenerator.generateKey(distributedLock.lockName(), arguments);

        CacheLock cacheLock = cacheService.getLock(key);
        if (Objects.isNull(cacheLock)) {
            throw new StratoException("Failed to obtain distributed lock by key: %s.".formatted(key));
        }

        boolean isLocked;
        try {

            if(distributedLock.waitIfLocked())
                isLocked = tryLock(cacheLock, maxWaitTime, maxLockSeconds);
            else
                isLocked = tryLockNoWait(cacheLock, maxLockSeconds);

            if (isLocked) {
                log.debug("Distributed lock {} acquired.", key);
                return joinPoint.proceed();
            }

            var failureHandler = distributedLock.failureHandler().getConstructor().newInstance();
            return failureHandler.handleLockFailure(joinPoint, distributedLock, key);
        } catch (Throwable throwable) {
            log.error("Distributed lock failure. Key={}.", key, throwable);
            if (distributedLock.ignoreError()) {
                return null;
            }
            throw new StratoException(throwable);
        } finally {
            if(distributedLock.unlockAfterDone()){
                cacheLock.unlock();
                log.debug("Distributed lock {} unlocked.", key);
            }
        }
    }



    private static DistributedLockKeyGenerator getKeyGenerator(DistributedLock distributedLock) {
        try {
            return distributedLock.keyGenerator().getConstructor().newInstance();
        } catch (Exception e) {
            throw new StratoException(e);
        }
    }

    private boolean tryLock(CacheLock cacheLock, int maxWaitTime, int lockSeconds) {
        return cacheLock.tryLock(maxWaitTime, lockSeconds);
    }

    private boolean tryLockNoWait(CacheLock cacheLock, int maxLockSeconds) {
        return cacheLock.tryLock(maxLockSeconds);
    }

}
