package com.stratocloud.lock;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DistributedLock {
    String lockName();

    Class<? extends DistributedLockKeyGenerator> keyGenerator() default DefaultDistributedLockKeyGenerator.class;

    Class<? extends DistributedLockFailureHandler> failureHandler() default DefaultDistributedLockFailureHandler.class;

    int maxWaitTime() default 25;

    int maxLockSeconds() default 20;

    boolean ignoreError() default false;

    boolean unlockAfterDone() default true;

    boolean waitIfLocked() default true;
}
