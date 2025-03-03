package com.stratocloud.tag;

import java.util.List;

public record TagEntryFilters(List<Long> entryIds,
                              List<String> entryKeys,
                              List<String> resourceCategories,
                              String search,
                              Boolean requiredWhenCreating,
                              Boolean requiredWhenFiltering,
                              Boolean userGroupTaggable) {
}
