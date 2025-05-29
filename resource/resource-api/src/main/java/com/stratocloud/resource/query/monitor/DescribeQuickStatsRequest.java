package com.stratocloud.resource.query.monitor;

import com.stratocloud.request.query.QueryRequest;
import com.stratocloud.utils.Assert;
import lombok.Data;

@Data
public class DescribeQuickStatsRequest implements QueryRequest {
    private Long resourceId;

    @Override
    public void validate() {
        Assert.isNotNull(resourceId);
    }
}
