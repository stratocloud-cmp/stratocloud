package com.stratocloud.tenant.query;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

@Data
public class DescribeInheritedTenantsRequest implements QueryRequest {
    private Long tenantId;
}
