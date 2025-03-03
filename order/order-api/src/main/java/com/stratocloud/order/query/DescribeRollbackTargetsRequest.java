package com.stratocloud.order.query;

import com.stratocloud.request.query.QueryRequest;
import com.stratocloud.utils.Assert;
import lombok.Data;

@Data
public class DescribeRollbackTargetsRequest implements QueryRequest {
    private Long orderId;

    @Override
    public void validate() {
        Assert.isNotNull(orderId);
    }
}
