package com.stratocloud.validate;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.request.query.QueryRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApiArgsValidator {

    @Around("@annotation(validateRequest)")
    public Object aroundApi(ProceedingJoinPoint joinPoint, ValidateRequest validateRequest) throws Throwable {
        validate(joinPoint);
        return joinPoint.proceed();
    }

    private static void validate(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        if(args != null) {
            for (Object arg : args) {
                if(arg instanceof ApiCommand apiCommand)
                    apiCommand.validate();
                else if(arg instanceof QueryRequest queryRequest)
                    queryRequest.validate();
            }
        }
    }

}
