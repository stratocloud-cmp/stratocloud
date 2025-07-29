package com.stratocloud.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NestedFormField {
    String label();

    String description() default "";

    String[] defaultJsonValues() default {};

    boolean multiple() default false;

    int multipleMin() default 0;

    int multipleMax() default 50;

    String[] conditions() default {};

    Class<? extends DynamicForm> nestedFormClass();
}
