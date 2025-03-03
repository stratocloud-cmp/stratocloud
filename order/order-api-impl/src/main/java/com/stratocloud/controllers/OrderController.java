package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.order.*;
import com.stratocloud.order.cmd.*;
import com.stratocloud.order.query.DescribeRollbackTargetsRequest;
import com.stratocloud.order.query.DescribeRollbackTargetsResponse;
import com.stratocloud.order.query.DescribeOrdersRequest;
import com.stratocloud.order.query.NestedOrderResponse;
import com.stratocloud.order.response.*;
import com.stratocloud.permission.PermissionTarget;
import com.stratocloud.permission.ReadPermissionRequired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "Order",targetName = "工单")
@RestController
public class OrderController implements OrderApi {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    @SendAuditLog(
            action = "CreateOrder",
            actionName = "创建工单",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public CreateOrderResponse createOrder(@RequestBody CreateOrderCmd cmd) {
        return orderService.createOrder(cmd);
    }

    @Override
    @SendAuditLog(
            action = "UpdateOrderItem",
            actionName = "修改工单项",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public UpdateOrderItemResponse updateOrderItem(UpdateOrderItemCmd cmd) {
        return orderService.updateOrderItem(cmd);
    }

    @Override
    @SendAuditLog(
            action = "SubmitOrder",
            actionName = "提交工单",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public SubmitOrderResponse submitOrder(@RequestBody SubmitOrderCmd cmd) {
        return orderService.submitOrder(cmd);
    }

    @Override
    @SendAuditLog(
            action = "ApproveOrder",
            actionName = "审批通过",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public ApproveOrderResponse approveOrder(@RequestBody ApproveOrderCmd cmd) {
        return orderService.approveOrder(cmd);
    }

    @Override
    @SendAuditLog(
            action = "ConfirmOrder",
            actionName = "确认工单",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public ConfirmOrderResponse confirmOrder(@RequestBody ConfirmOrderCmd cmd) {
        return orderService.confirmOrder(cmd);
    }

    @Override
    @SendAuditLog(
            action = "RollbackOrder",
            actionName = "回退工单",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public RollbackOrderResponse rollbackOrder(@RequestBody RollbackOrderCmd cmd) {
        return orderService.rollbackOrder(cmd);
    }


    @Override
    @SendAuditLog(
            action = "RejectOrder",
            actionName = "驳回工单",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public RejectOrderResponse rejectOrder(@RequestBody RejectOrderCmd cmd) {
        return orderService.rejectOrder(cmd);
    }

    @Override
    @SendAuditLog(
            action = "CancelOrder",
            actionName = "取消工单",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public CancelOrderResponse cancelOrder(@RequestBody CancelOrderCmd cmd) {
        return orderService.cancelOrder(cmd);
    }

    @Override
    @SendAuditLog(
            action = "DenyOrder",
            actionName = "拒绝工单",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public DenyOrderResponse denyOrder(@RequestBody DenyOrderCmd cmd) {
        return orderService.denyOrder(cmd);
    }

    @Override
    @SendAuditLog(
            action = "CloneOrder",
            actionName = "克隆工单",
            objectType = "Order",
            objectTypeName = "工单"
    )
    public CloneOrderResponse cloneOrder(@RequestBody CloneOrderCmd cmd) {
        return orderService.cloneOrder(cmd);
    }

    @Override
    @ReadPermissionRequired
    public Page<NestedOrderResponse> describeOrders(@RequestBody DescribeOrdersRequest request) {
        return orderService.describeOrders(request);
    }


    @Override
    @ReadPermissionRequired
    public DescribeRollbackTargetsResponse describeRollbackTargets(@RequestBody DescribeRollbackTargetsRequest request) {
        return orderService.describeRollbackTargets(request);
    }
}
