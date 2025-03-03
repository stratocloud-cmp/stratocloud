package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

@Data
public class DescribeResourceTypesRequest implements QueryRequest {
    private String resourceCategoryId;
    private String providerId;
    private String resourceTypeId;
}
