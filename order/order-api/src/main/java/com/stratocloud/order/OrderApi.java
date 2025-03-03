package com.stratocloud.order;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.order.cmd.*;
import com.stratocloud.order.query.DescribeRollbackTargetsRequest;
import com.stratocloud.order.query.DescribeRollbackTargetsResponse;
import com.stratocloud.order.query.DescribeOrdersRequest;
import com.stratocloud.order.query.NestedOrderResponse;
import com.stratocloud.order.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface OrderApi {
    @PostMapping(path = StratoServices.ORDER_SERVICE+"/create-order")
    CreateOrderResponse createOrder(@RequestBody CreateOrderCmd cmd);
    @PostMapping(path = StratoServices.ORDER_SERVICE+"/update-order-item")
    UpdateOrderItemResponse updateOrderItem(@RequestBody UpdateOrderItemCmd cmd);
    @PostMapping(path = StratoServices.ORDER_SERVICE+"/submit-order")
    SubmitOrderResponse submitOrder(@RequestBody SubmitOrderCmd cmd);
    @PostMapping(path = StratoServices.ORDER_SERVICE+"/approve-order")
    ApproveOrderResponse approveOrder(@RequestBody ApproveOrderCmd cmd);
    @PostMapping(path = StratoServices.ORDER_SERVICE+"/confirm-order")
    ConfirmOrderResponse confirmOrder(@RequestBody ConfirmOrderCmd cmd);
    @PostMapping(path = StratoServices.ORDER_SERVICE+"/rollback-order")
    RollbackOrderResponse rollbackOrder(@RequestBody RollbackOrderCmd cmd);

    @PostMapping(path = StratoServices.ORDER_SERVICE+"/reject-order")
    RejectOrderResponse rejectOrder(@RequestBody RejectOrderCmd cmd);

    @PostMapping(path = StratoServices.ORDER_SERVICE+"/cancel-order")
    CancelOrderResponse cancelOrder(@RequestBody CancelOrderCmd cmd);

    @PostMapping(path = StratoServices.ORDER_SERVICE+"/deny-order")
    DenyOrderResponse denyOrder(@RequestBody DenyOrderCmd cmd);

    @PostMapping(path = StratoServices.ORDER_SERVICE+"/clone-order")
    CloneOrderResponse cloneOrder(@RequestBody CloneOrderCmd cmd);

    @PostMapping(path = StratoServices.ORDER_SERVICE+"/describe-orders")
    Page<NestedOrderResponse> describeOrders(@RequestBody DescribeOrdersRequest request);

    @PostMapping(path = StratoServices.ORDER_SERVICE+"/describe-order-rollback-targets")
    DescribeRollbackTargetsResponse describeRollbackTargets(@RequestBody DescribeRollbackTargetsRequest request);
}
