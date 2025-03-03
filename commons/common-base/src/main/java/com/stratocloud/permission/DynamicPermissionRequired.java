package com.stratocloud.permission;

import com.stratocloud.auth.CallContext;

public interface DynamicPermissionRequired {
    PermissionItem getPermissionItem();

    default void validatePermission(){
        PermissionItem item = getPermissionItem();
        CallContext.current().validatePermission(item.target(), item.action());
    }
}
