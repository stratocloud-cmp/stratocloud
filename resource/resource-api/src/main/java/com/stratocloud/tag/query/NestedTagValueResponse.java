package com.stratocloud.tag.query;

import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedTagValueResponse extends NestedTenanted {
    private Long tagEntryId;
    private String tagKey;
    private String tagKeyName;

    private String tagValue;
    private String tagValueName;
    private String description;
}
