package com.stratocloud.order;

import java.util.List;

public record OrderFilters(List<Long> orderIds, List<Long> tenantIds, List<Long> ownerIds,
                           List<Long> possibleHandlerIds, List<Long> historyHandlerIds,
                           List<OrderStatus> orderStatuses, String search,
                           List<Long> workflowIds) {
}
