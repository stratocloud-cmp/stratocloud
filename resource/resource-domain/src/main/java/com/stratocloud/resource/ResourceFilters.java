package com.stratocloud.resource;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ResourceFilters(String search,
                              Boolean recycled,

                              List<ResourceState> states,
                              List<ResourceSyncState> syncStates,

                              List<Long> resourceIds,
                              List<Long> requirementTargetIds,
                              List<Long> tenantIds,
                              List<Long> ownerIds,
                              List<String> providerIds,
                              List<Long> accountIds,
                              List<String> resourceCategories,
                              List<String> resourceTypes,
                              Map<String, List<String>> tagsMap,
                              Boolean ipPoolAttachable) {

}