package com.stratocloud.tag.query;

import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedTagEntryResponse extends NestedTenanted {
    private String resourceCategory;
    private String tagKey;
    private String tagKeyName;

    private String description;

    private Boolean requiredWhenCreating;

    private Boolean requiredWhenFiltering;


    private Boolean userGroupTaggable;
}
