package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.cart.CartApi;
import com.stratocloud.cart.CartService;
import com.stratocloud.cart.cmd.CreateCartItemCmd;
import com.stratocloud.cart.cmd.DeleteCartItemsCmd;
import com.stratocloud.cart.cmd.SubmitCartItemsCmd;
import com.stratocloud.cart.cmd.UpdateCartItemCmd;
import com.stratocloud.cart.query.DescribeAllCartItemsResponse;
import com.stratocloud.cart.query.DescribeCartItemsRequest;
import com.stratocloud.cart.query.NestedCartItemResponse;
import com.stratocloud.cart.response.CreateCartItemResponse;
import com.stratocloud.cart.response.DeleteCartItemsResponse;
import com.stratocloud.cart.response.SubmitCartItemsResponse;
import com.stratocloud.cart.response.UpdateCartItemResponse;
import com.stratocloud.permission.PermissionTarget;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "Cart", targetName = "任务队列")
@RestController
public class CartController implements CartApi {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @Override
    public Page<NestedCartItemResponse> describeCartItems(@RequestBody DescribeCartItemsRequest request) {
        return service.describeCartItems(request);
    }

    @Override
    public DescribeAllCartItemsResponse describeAllCartItems(@RequestBody DescribeCartItemsRequest request) {
        return service.describeAllCartItems(request);
    }

    @Override
    @SendAuditLog(
            action = "CreateCartItem",
            actionName = "添加待办任务",
            objectType = "CartItem",
            objectTypeName = "待办任务"
    )
    public CreateCartItemResponse createCartItem(@RequestBody CreateCartItemCmd cmd) {
        return service.createCartItem(cmd);
    }

    @Override
    @SendAuditLog(
            action = "UpdateCartItem",
            actionName = "更新待办任务",
            objectType = "CartItem",
            objectTypeName = "待办任务"
    )
    public UpdateCartItemResponse updateCartItem(@RequestBody UpdateCartItemCmd cmd) {
        return service.updateCartItem(cmd);
    }

    @Override
    @SendAuditLog(
            action = "DeleteCartItems",
            actionName = "删除待办任务",
            objectType = "CartItem",
            objectTypeName = "待办任务"
    )
    public DeleteCartItemsResponse deleteCartItems(@RequestBody DeleteCartItemsCmd cmd) {
        return service.deleteCartItems(cmd);
    }

    @Override
    @SendAuditLog(
            action = "SubmitCartItems",
            actionName = "提交待办任务",
            objectType = "CartItem",
            objectTypeName = "待办任务"
    )
    public SubmitCartItemsResponse submitCartItems(@RequestBody SubmitCartItemsCmd cmd) {
        return service.submitCartItems(cmd);
    }
}
