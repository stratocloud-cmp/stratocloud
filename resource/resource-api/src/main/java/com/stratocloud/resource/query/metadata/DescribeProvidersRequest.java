package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

import java.util.List;

@Data
public class DescribeProvidersRequest implements QueryRequest {
    private List<String> providerIds;
    private String resourceCategory;
}
