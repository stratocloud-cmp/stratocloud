package com.stratocloud.stack.runtime.query;

import com.stratocloud.request.query.PagingRequest;
import com.stratocloud.stack.ResourceStackState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DescribeResourceStacksRequest extends PagingRequest {
    private String search;
    private Boolean recycled;

    private List<ResourceStackState> states;

    private List<Long> tenantIds;
    private List<Long> ownerIds;

    private List<Long> resourceStackIds;

    private Map<String, List<String>> tagsMap;
}
