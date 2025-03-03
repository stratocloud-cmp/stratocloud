package com.stratocloud.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InputField {
    String label();

    String description() default "";

    String defaultValue() default "";

    String inputType() default "text";

    boolean required() default true;

    String[] conditions() default {};

    String regex() default "";

    String regexMessage() default "";
}
