package com.stratocloud.tenant.query;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

@Data
public class DescribeSubTenantsRequest implements QueryRequest {
    private Long tenantId;
}
