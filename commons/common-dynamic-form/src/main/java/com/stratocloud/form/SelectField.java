package com.stratocloud.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SelectField {
    String label();

    String description() default "";

    boolean multiSelect() default false;

    boolean allowCreate() default false;

    String[] defaultValues() default {};

    String[] options() default {};

    String[] optionNames() default {};


    Source source() default Source.STATIC;

    SelectEntityType entityType() default SelectEntityType.NONE;


    String[] dependsOn() default {};

    boolean required() default true;

    String[] conditions() default {};

    SelectType type() default SelectType.NORMAL;
}
