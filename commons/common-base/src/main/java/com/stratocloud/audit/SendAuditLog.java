package com.stratocloud.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SendAuditLog {
    String action();
    String actionName();
    String objectType();
    String objectTypeName();

    boolean hideRequestBody() default false;
    boolean hideResponseBody() default false;
}
