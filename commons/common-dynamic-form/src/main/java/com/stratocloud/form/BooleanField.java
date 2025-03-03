package com.stratocloud.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BooleanField {
    String label();

    String description() default "";

    boolean defaultValue() default false;

    String[] conditions() default {};
}
