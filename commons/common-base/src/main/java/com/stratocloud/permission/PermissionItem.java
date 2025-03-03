package com.stratocloud.permission;

public record PermissionItem(String target,
                             String targetName,
                             String action,
                             String actionName) {
}
