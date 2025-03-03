package com.stratocloud.limit.query;

import com.stratocloud.request.query.NestedTenanted;
import com.stratocloud.limit.cmd.NestedUsageLimitItem;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedLimitResponse extends NestedTenanted {
    private String name;
    private String description;
    private Boolean disabled = false;
    private List<Long> ownerIds;
    private List<String> providerIds;
    private List<Long> accountIds;
    private List<String> resourceCategories;
    private List<NestedResourceTag> tags;
    private List<NestedUsageLimitItem> items;
}
