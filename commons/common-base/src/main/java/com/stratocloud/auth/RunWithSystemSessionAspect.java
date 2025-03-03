package com.stratocloud.auth;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(1)
public class RunWithSystemSessionAspect {
    @Around("@annotation(runWithSystemSession)")
    public Object around(ProceedingJoinPoint joinPoint, RunWithSystemSession runWithSystemSession) throws Throwable {
        CallContext.registerSystemSession();
        try {
            return joinPoint.proceed();
        }finally {
            CallContext.unregister();
        }
    }
}
