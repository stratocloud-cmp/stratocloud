package com.stratocloud.order;

import com.stratocloud.external.order.UserGatewayService;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.order.query.*;
import com.stratocloud.utils.Utils;
import com.stratocloud.workflow.runtime.RollbackTarget;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderAssembler {

    private final UserGatewayService userGatewayService;

    public OrderAssembler(UserGatewayService userGatewayService) {
        this.userGatewayService = userGatewayService;
    }

    public NestedOrderResponse toNestedOrderResponse(Order order) {
        NestedOrderResponse r = new NestedOrderResponse();

        EntityUtil.copyBasicFields(order, r);

        r.setOrderNo(order.getOrderNo());
        r.setOrderName(order.getOrderName());
        r.setNote(order.getNote());
        r.setStatus(order.getStatus());
        r.setErrorMessage(order.getErrorMessage());
        r.setUserMessage(order.getUserMessage());
        r.setSummary(order.getSummary());
        r.setPossibleHandlers(toNestedPossibleHandlers(order.getPossibleHandlers()));
        r.setSubmittedAt(order.getSubmittedAt());
        r.setLastApprovedAt(order.getLastApprovedAt());
        r.setEndedAt(order.getEndedAt());
        r.setOrderItems(toNestedItems(order.getOrderItems()));
        r.setWorkflowId(order.getWorkflow().getId());
        r.setWorkflowName(order.getWorkflow().getName());

        return r;
    }

    private List<NestedOrderItem> toNestedItems(List<OrderItem> orderItems) {
        List<NestedOrderItem> result = new ArrayList<>();
        if(Utils.isEmpty(orderItems))
            return result;
        for (OrderItem item : orderItems) {
            result.add(toNestedItem(item));
        }
        return result;
    }

    private NestedOrderItem toNestedItem(OrderItem item) {
        NestedOrderItem nestedOrderItem = new NestedOrderItem();
        EntityUtil.copyBasicFields(item, nestedOrderItem);

        if(item.getJobNodeInstance()!=null)
            nestedOrderItem.setJobId(item.getJobNodeInstance().getJob().getId());

        nestedOrderItem.setJobType(item.getJobNode().getJobDefinition().getJobType());
        nestedOrderItem.setJobTypeName(item.getJobNode().getJobDefinition().getJobTypeName());
        nestedOrderItem.setNodeName(item.getJobNode().getName());
        nestedOrderItem.setParameters(item.getParameters());
        return nestedOrderItem;
    }

    private List<NestedPossibleHandler> toNestedPossibleHandlers(List<PossibleHandler> possibleHandlers) {
        List<NestedPossibleHandler> result = new ArrayList<>();
        if(Utils.isEmpty(possibleHandlers))
            return result;
        for (PossibleHandler possibleHandler : possibleHandlers) {
            result.add(toNestedPossibleHandler(possibleHandler));
        }
        return result;
    }

    private NestedPossibleHandler toNestedPossibleHandler(PossibleHandler possibleHandler) {
        NestedPossibleHandler nestedPossibleHandler = new NestedPossibleHandler();
        nestedPossibleHandler.setUserId(possibleHandler.getUserId());
        nestedPossibleHandler.setUserName(possibleHandler.getUserName());
        nestedPossibleHandler.setNodeInstanceId(possibleHandler.getNodeInstanceId());
        return nestedPossibleHandler;
    }

    public DescribeRollbackTargetsResponse toDescribeRollbackTargetsResponse(Order order) {
        List<NestedRollbackTarget> result = new ArrayList<>();
        List<RollbackTarget> rollbackTargets = order.getRollbackTargets();

        for (RollbackTarget rollbackTarget : rollbackTargets) {
            NestedRollbackTarget nestedRollbackTarget = new NestedRollbackTarget();
            nestedRollbackTarget.setNodeId(rollbackTarget.getNodeId());
            nestedRollbackTarget.setNodeName(rollbackTarget.getNodeName());
            nestedRollbackTarget.setNodeInstanceId(rollbackTarget.getNodeInstanceId());
            nestedRollbackTarget.setPossibleHandlers(rollbackTarget.getPossibleHandlers());
            result.add(nestedRollbackTarget);
        }

        return new DescribeRollbackTargetsResponse(result);
    }

    public Page<NestedOrderResponse> convertPage(Page<Order> page) {
        Page<NestedOrderResponse> responsePage = page.map(this::toNestedOrderResponse);
        EntityUtil.fillOwnerInfo(responsePage, userGatewayService);
        return responsePage;
    }
}
