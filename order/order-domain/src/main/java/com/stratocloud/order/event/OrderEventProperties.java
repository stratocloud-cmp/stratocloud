package com.stratocloud.order.event;

import com.stratocloud.event.EventProperties;
import com.stratocloud.job.JobDefinition;
import com.stratocloud.order.Order;
import com.stratocloud.order.OrderItem;
import com.stratocloud.order.OrderStatus;
import com.stratocloud.order.PossibleHandler;
import com.stratocloud.utils.Utils;
import com.stratocloud.workflow.nodes.JobNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class OrderEventProperties implements EventProperties {
    private Long orderId;
    private String orderNo;
    private String orderName;
    private String note;

    private OrderStatus orderStatus;

    private String errorMessage;
    private String userMessage;
    private String summary;


    private List<Long> possibleHandlerIds;
    private List<String> possibleHandlerNames;

    private LocalDateTime submittedAt;
    private LocalDateTime lastApprovedAt;
    private LocalDateTime endedAt;

    private Long workflowId;
    private String workflowName;

    private Map<String, Object> runtimeProperties;

    private List<OrderItemProperties> orderItems;

    public static OrderEventProperties create(Order order){
        OrderEventProperties eventProperties = new OrderEventProperties();
        eventProperties.setOrderId(order.getId());
        eventProperties.setOrderNo(order.getOrderNo());
        eventProperties.setOrderName(order.getOrderName());
        eventProperties.setNote(order.getNote());
        eventProperties.setOrderStatus(order.getStatus());
        eventProperties.setErrorMessage(order.getErrorMessage());
        eventProperties.setUserMessage(order.getUserMessage());
        eventProperties.setSummary(order.getSummary());

        eventProperties.setSubmittedAt(order.getSubmittedAt());
        eventProperties.setLastApprovedAt(order.getLastApprovedAt());
        eventProperties.setEndedAt(order.getEndedAt());
        eventProperties.setWorkflowId(order.getWorkflow().getId());
        eventProperties.setWorkflowName(order.getWorkflow().getName());
        eventProperties.setRuntimeProperties(order.getWorkflowInstance().getRuntimeProperties());

        if(Utils.isNotEmpty(order.getPossibleHandlers())){
            eventProperties.setPossibleHandlerIds(
                    order.getPossibleHandlers().stream().map(PossibleHandler::getUserId).toList()
            );
            eventProperties.setPossibleHandlerNames(
                    order.getPossibleHandlers().stream().map(PossibleHandler::getUserName).toList()
            );
        }

        if(Utils.isNotEmpty(order.getOrderItems())){
            eventProperties.setOrderItems(
                    order.getOrderItems().stream().map(OrderEventProperties::convertOrderItem).toList()
            );
        }


        return eventProperties;
    }

    private static OrderItemProperties convertOrderItem(OrderItem i) {
        JobNode jobNode = i.getJobNode();
        JobDefinition jobDefinition = jobNode.getJobDefinition();
        return new OrderItemProperties(
                jobNode.getNodeType(),
                jobNode.getNodeKey(),
                jobNode.getName(),
                jobDefinition.getJobType(),
                jobDefinition.getJobTypeName(),
                i.getParameters()
        );
    }

    public static OrderEventProperties createExample() {
        OrderEventProperties eventProperties = new OrderEventProperties();
        eventProperties.setOrderId(0L);
        eventProperties.setOrderNo("");
        eventProperties.setOrderName("");
        eventProperties.setNote("");
        eventProperties.setOrderStatus(OrderStatus.AWAIT_SUBMIT);
        eventProperties.setErrorMessage("");
        eventProperties.setUserMessage("");
        eventProperties.setSummary("");
        eventProperties.setPossibleHandlerIds(List.of());
        eventProperties.setPossibleHandlerNames(List.of());
        eventProperties.setSubmittedAt(LocalDateTime.now());
        eventProperties.setLastApprovedAt(LocalDateTime.now());
        eventProperties.setEndedAt(LocalDateTime.now());
        eventProperties.setWorkflowId(0L);
        eventProperties.setWorkflowName("");
        eventProperties.setRuntimeProperties(Map.of());

        OrderItemProperties itemProperties = new OrderItemProperties(
                "", "", "", "", "", Map.of()
        );

        eventProperties.setOrderItems(List.of(itemProperties));

        return eventProperties;
    }
}
