package com.stratocloud.resource.event;

import com.stratocloud.event.EventProperties;
import com.stratocloud.resource.ResourceCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ResourceEventProperties implements EventProperties {
    private String providerId;
    private String providerName;

    private Long accountId;
    private String accountName;

    private ResourceCategory resourceCategory;
    private String resourceTypeId;
    private String resourceTypeName;

    private Long resourceId;
    private String resourceName;

    private Long resourceOwnerId;
    private Long resourceTenantId;
}
