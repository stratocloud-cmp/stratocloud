package com.stratocloud.limit.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.resource.cmd.create.NestedResourceTag;
import lombok.Data;

import java.util.List;

@Data
public class UpdateLimitCmd implements ApiCommand {
    private Long limitId;

    private String name;
    private String description;
    private List<Long> ownerIds;
    private List<String> providerIds;
    private List<Long> accountIds;
    private List<String> resourceCategories;

    private List<NestedResourceTag> tags;
    private List<NestedUsageLimitItem> items;
}
