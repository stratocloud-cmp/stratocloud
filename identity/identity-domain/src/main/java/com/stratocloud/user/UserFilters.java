package com.stratocloud.user;

import java.util.List;

public record UserFilters(List<Long> tenantIds,
                          List<Long> userIds,
                          List<Long> roleIds,
                          List<Long> userGroupIds,
                          String search,
                          Boolean disabled,
                          Boolean locked) {
}