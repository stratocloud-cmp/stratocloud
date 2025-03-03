package com.stratocloud.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NestedField {
    String label();

    String description() default "";

    boolean multiple() default false;

    String[] dependsOn() default {};

    boolean required() default true;

    String[] conditions() default {};
}
