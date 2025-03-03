package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.query.QueryRequest;
import com.stratocloud.utils.Assert;
import lombok.Data;

@Data
public class DescribeRelationshipFormRequest implements QueryRequest {
    private String relationshipTypeId;

    @Override
    public void validate() {
        Assert.isNotBlank(relationshipTypeId);
    }
}
