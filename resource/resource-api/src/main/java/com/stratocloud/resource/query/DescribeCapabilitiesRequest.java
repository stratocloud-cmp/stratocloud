package com.stratocloud.resource.query;

import com.stratocloud.request.query.PagingRequest;
import com.stratocloud.utils.Assert;
import com.stratocloud.utils.Utils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DescribeCapabilitiesRequest extends PagingRequest {
    private Long targetId;
    private String relationshipType;
    private String search;

    @Override
    public void validate() {
        Assert.isTrue(targetId != null, "Property targetId cannot be null.");
        Assert.isTrue(Utils.isNotBlank(relationshipType), "Property relationshipType cannot be empty.");
    }
}
