package com.stratocloud.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PermissionRequired(action = "UPDATE", actionName = "更新")
public @interface UpdatePermissionRequired {
}
