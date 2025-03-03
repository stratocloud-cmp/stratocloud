package com.stratocloud.resource.query;

import com.stratocloud.request.query.NestedControllable;
import com.stratocloud.resource.ResourceState;
import com.stratocloud.resource.ResourceSyncState;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class NestedResourceResponse extends NestedControllable {
    private String providerId;
    private String providerName;
    private Long accountId;
    private String category;
    private String categoryName;
    private String type;
    private String typeName;
    private String externalId;
    private String name;
    private String description;
    private ResourceState state;
    private ResourceSyncState syncState;

    private List<NestedRuntimeProperty> runtimeProperties;
    private List<NestedResourceUsage> allocatedUsages;
    private List<NestedResourceUsage> preAllocatedUsages;

    private List<NestedResourceTag> tags;

    private Boolean recycled = false;

    private LocalDateTime recycledTime;

    private String costDescription;
    private String monthlyCostDescription;
}
