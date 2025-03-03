package com.stratocloud.role;

import com.stratocloud.identity.RoleType;

import java.util.List;

public record RoleFilters(List<Long> roleIds, List<RoleType> roleTypes, String search, List<Long> userIds) {
}
