package com.stratocloud.tenant;

import java.util.List;

public record TenantFilters(List<Long> tenantIds, String search, Boolean disabled, List<Long> parentIds,
                            Boolean includeInherited) {
}