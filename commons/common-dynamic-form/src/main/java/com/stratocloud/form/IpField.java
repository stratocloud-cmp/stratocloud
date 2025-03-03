package com.stratocloud.form;

import com.stratocloud.ip.InternetProtocol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IpField {
    String label();

    String description() default "";

    String placeHolder() default "";

    int multipleLimit() default 20;

    InternetProtocol protocol() default InternetProtocol.IPv4;

    boolean required() default false;

    String[] conditions() default {};
}
