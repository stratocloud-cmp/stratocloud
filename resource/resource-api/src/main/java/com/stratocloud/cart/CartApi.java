package com.stratocloud.cart;

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
import com.stratocloud.constant.StratoServices;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface CartApi {

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-cart-items")
    Page<NestedCartItemResponse> describeCartItems(@RequestBody DescribeCartItemsRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-all-cart-items")
    DescribeAllCartItemsResponse describeAllCartItems(@RequestBody DescribeCartItemsRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/create-cart-item")
    CreateCartItemResponse createCartItem(@RequestBody CreateCartItemCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/update-cart-item")
    UpdateCartItemResponse updateCartItem(@RequestBody UpdateCartItemCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/delete-cart-items")
    DeleteCartItemsResponse deleteCartItems(@RequestBody DeleteCartItemsCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/submit-cart-items")
    SubmitCartItemsResponse submitCartItems(@RequestBody SubmitCartItemsCmd cmd);
}
