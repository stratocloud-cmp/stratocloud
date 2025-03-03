package com.stratocloud.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NumberField {
    String label();

    String description() default "";

    String placeHolder() default "";

    int[] defaultValue() default {};

    int min() default 0;
    int max() default Integer.MAX_VALUE;

    boolean required() default true;


    String[] conditions() default {};
}
