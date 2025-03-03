package com.stratocloud.tenant.query;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

@Data
public class DescribeTenantsTreeRequest implements QueryRequest {
    private Boolean includeInherited = false;
}
