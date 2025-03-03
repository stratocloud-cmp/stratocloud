package com.stratocloud.permission;

import com.stratocloud.auth.CheckToken;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@CheckToken
public @interface PermissionTarget {
    String target();
    String targetName();
}
