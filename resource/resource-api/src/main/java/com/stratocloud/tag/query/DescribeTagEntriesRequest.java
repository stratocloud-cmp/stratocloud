package com.stratocloud.tag.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeTagEntriesRequest extends PagingRequest {
    private List<Long> entryIds;

    private List<String> entryKeys;

    private List<String> resourceCategories;

    private String search;

    private Boolean disabled = false;

    private Boolean requiredWhenCreating;
    private Boolean requiredWhenFiltering;

    private Boolean userGroupTaggable;
}
