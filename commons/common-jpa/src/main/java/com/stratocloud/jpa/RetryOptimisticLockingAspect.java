package com.stratocloud.jpa;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.concurrent.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Aspect
@Slf4j
@Component
public class RetryOptimisticLockingAspect {

    public static final int MAX_RETRIES = 5;

    @Around("@annotation(retryOptimisticLocking)")
    public Object around(ProceedingJoinPoint joinPoint, RetryOptimisticLocking retryOptimisticLocking) {
        int count = 0;
        while (count < MAX_RETRIES){
            try {
                return joinPoint.proceed();
            }catch (ObjectOptimisticLockingFailureException e){
                count++;
                if(count < MAX_RETRIES){
                    log.warn("Optimistic locking failure of {}, retrying. Count={}.",
                            joinPoint.getSignature().getName(), count);
                    SleepUtil.sleep(3);
                }else {
                    log.error("Failed to obtain optimistic lock of {} after {} tries, aborting now.",
                            joinPoint.getSignature().getName(), count);
                }
            }catch (Throwable e){
                if(e instanceof RuntimeException runtimeException)
                    throw runtimeException;
                throw new StratoException(e.getMessage(), e);
            }
        }

        throw new StratoException(
                "Failed to obtain optimistic lock of %s.".formatted(joinPoint.getSignature().getName())
        );
    }


}
