package com.stratocloud.resource.query.monitor;

import com.stratocloud.request.query.QueryRequest;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DescribeMetricsRequest implements QueryRequest {
    private Long resourceId;

    private LocalDateTime from;

    private LocalDateTime to;

    @Override
    public void validate() {
        Assert.isNotNull(resourceId);
    }
}
