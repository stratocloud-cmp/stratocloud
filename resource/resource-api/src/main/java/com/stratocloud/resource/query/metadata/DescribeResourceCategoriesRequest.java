package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

@Data
public class DescribeResourceCategoriesRequest implements QueryRequest {
    private String providerId;

    private String categoryId;
}
