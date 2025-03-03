package com.stratocloud.tenant.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeTenantsRequest extends PagingRequest {
    private List<Long> tenantIds;
    private String search;
    private Boolean disabled;
    private List<Long> parentIds;
    private Boolean includeInherited = true;
}
