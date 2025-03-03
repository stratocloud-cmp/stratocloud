package com.stratocloud.cart.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DescribeCartItemsRequest extends PagingRequest {
    private String search;
}
