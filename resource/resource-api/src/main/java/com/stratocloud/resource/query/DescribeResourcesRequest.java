package com.stratocloud.resource.query;

import com.stratocloud.request.query.PagingRequest;
import com.stratocloud.resource.ResourceState;
import com.stratocloud.resource.ResourceSyncState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DescribeResourcesRequest extends PagingRequest {
    private String search;
    private Boolean recycled;

    private List<ResourceState> states;
    private List<ResourceSyncState> syncStates;

    private List<Long> resourceIds;
    private List<Long> requirementTargetIds;
    private List<Long> tenantIds;
    private List<Long> ownerIds;

    private List<String> providerIds;
    private List<Long> accountIds;

    private List<String> resourceCategories;
    private List<String> resourceTypes;

    private Map<String, List<String>> tagsMap;

    private Boolean ipPoolAttachable;
}
