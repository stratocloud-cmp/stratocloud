package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

@Data
public class DescribeRelationshipTypesRequest implements QueryRequest {
    private String sourceTypeId;
    private String targetTypeId;
}
