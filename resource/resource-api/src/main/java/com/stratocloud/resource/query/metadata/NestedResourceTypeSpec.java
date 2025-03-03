package com.stratocloud.resource.query.metadata;

import lombok.Data;

@Data
public class NestedResourceTypeSpec {
    private String resourceCategoryId;
    private String resourceCategoryName;
    private String providerId;
    private String providerName;
    private String resourceTypeId;
    private String resourceTypeName;

    private Boolean manageable;
    private Boolean infrastructure;
    private Boolean sharedRequirementTarget;

    private Boolean buildable;
    private Boolean destroyable;

    private Boolean canAttachIpPool;
}
