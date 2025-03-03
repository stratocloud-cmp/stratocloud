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
import org.springframework.data.domain.Page;

public interface CartService {
    Page<NestedCartItemResponse> describeCartItems(DescribeCartItemsRequest request);

    CreateCartItemResponse createCartItem(CreateCartItemCmd cmd);

    UpdateCartItemResponse updateCartItem(UpdateCartItemCmd cmd);

    DeleteCartItemsResponse deleteCartItems(DeleteCartItemsCmd cmd);

    SubmitCartItemsResponse submitCartItems(SubmitCartItemsCmd cmd);

    DescribeAllCartItemsResponse describeAllCartItems(DescribeCartItemsRequest request);
}
