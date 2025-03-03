package com.stratocloud.order.query;

import com.stratocloud.request.query.NestedControllable;
import com.stratocloud.order.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class NestedOrderResponse extends NestedControllable {
    private String orderNo;
    private String orderName;
    private String note;
    private OrderStatus status;
    private String errorMessage;
    private String userMessage;
    private String summary;
    private List<NestedPossibleHandler> possibleHandlers;
    private LocalDateTime submittedAt;
    private LocalDateTime lastApprovedAt;
    private LocalDateTime endedAt;
    private List<NestedOrderItem> orderItems;

    private Long workflowId;
    private String workflowName;
}
