package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

@Data
public class DescribeRelationshipSpecRequest implements QueryRequest {
    private String relationshipTypeId;
}
