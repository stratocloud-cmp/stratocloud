package com.stratocloud.resource.query.metadata;

import lombok.Data;

@Data
public class NestedResourceCategory {
    private String categoryId;
    private String categoryName;

    private String groupId;
    private String groupName;
}
