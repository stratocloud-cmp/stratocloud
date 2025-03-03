package com.stratocloud.tenant.query;

import com.stratocloud.request.query.NestedAuditable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedTenantResponse extends NestedAuditable {
    private String name;
    private String description;
    private Boolean disabled;

    private Long parentId;
    private String parentName;
}
