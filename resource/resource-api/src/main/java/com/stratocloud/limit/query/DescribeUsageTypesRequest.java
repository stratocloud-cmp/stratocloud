package com.stratocloud.limit.query;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

import java.util.List;

@Data
public class DescribeUsageTypesRequest implements QueryRequest {
    private List<String> providerIds;
    private List<String> resourceCategories;
}
