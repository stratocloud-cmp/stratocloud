package com.stratocloud.cart.query;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeAllCartItemsResponse extends ApiResponse {
    private List<NestedCartItemResponse> cartItems;
}
