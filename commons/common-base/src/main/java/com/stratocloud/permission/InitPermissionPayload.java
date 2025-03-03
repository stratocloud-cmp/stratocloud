package com.stratocloud.permission;

import java.util.List;

public record InitPermissionPayload(List<PermissionItem> items) {
}
