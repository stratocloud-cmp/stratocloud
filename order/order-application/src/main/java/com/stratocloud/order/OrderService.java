package com.stratocloud.order;

import com.stratocloud.order.cmd.*;
import com.stratocloud.order.query.DescribeRollbackTargetsRequest;
import com.stratocloud.order.query.DescribeRollbackTargetsResponse;
import com.stratocloud.order.query.DescribeOrdersRequest;
import com.stratocloud.order.query.NestedOrderResponse;
import com.stratocloud.order.response.*;
import org.springframework.data.domain.Page;

public interface OrderService {
    CreateOrderResponse createOrder(CreateOrderCmd cmd);

    UpdateOrderItemResponse updateOrderItem(UpdateOrderItemCmd cmd);

    SubmitOrderResponse submitOrder(SubmitOrderCmd cmd);

    ApproveOrderResponse approveOrder(ApproveOrderCmd cmd);

    ConfirmOrderResponse confirmOrder(ConfirmOrderCmd cmd);

    RollbackOrderResponse rollbackOrder(RollbackOrderCmd cmd);

    RejectOrderResponse rejectOrder(RejectOrderCmd cmd);

    CancelOrderResponse cancelOrder(CancelOrderCmd cmd);

    DenyOrderResponse denyOrder(DenyOrderCmd cmd);

    Page<NestedOrderResponse> describeOrders(DescribeOrdersRequest request);

    DescribeRollbackTargetsResponse describeRollbackTargets(DescribeRollbackTargetsRequest request);

    CloneOrderResponse cloneOrder(CloneOrderCmd cmd);
}
