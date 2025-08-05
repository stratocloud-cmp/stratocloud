package com.stratocloud.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DateTimeField {
    String label();

    String description() default "";

    String placeHolder() default "";

    long[] defaultValue() default {};

    boolean allowFutureTime() default true;

    boolean required() default true;


    String[] conditions() default {};
}
