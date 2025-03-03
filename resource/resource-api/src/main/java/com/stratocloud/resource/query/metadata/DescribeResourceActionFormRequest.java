package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.query.QueryRequest;
import com.stratocloud.utils.Assert;
import com.stratocloud.utils.Utils;
import lombok.Data;

@Data
public class DescribeResourceActionFormRequest implements QueryRequest {
    private String resourceTypeId;
    private Long resourceId;
    private String actionId;

    @Override
    public void validate() {
        Assert.isTrue(
                Utils.isNotBlank(resourceTypeId)||resourceId!=null,
                "Must provide a resourceTypeId or a resourceId"
        );
        Assert.isNotBlank(actionId);
    }
}
