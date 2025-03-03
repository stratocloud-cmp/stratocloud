package com.stratocloud.order.query;

import com.stratocloud.request.query.PagingRequest;
import com.stratocloud.order.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeOrdersRequest extends PagingRequest {
    private List<Long> orderIds;
    private List<Long> tenantIds;
    private List<Long> ownerIds;
    private List<Long> possibleHandlerIds;
    private List<Long> historyHandlerIds;
    private List<OrderStatus> orderStatuses;
    private String search;

    private List<Long> workflowIds;
}
