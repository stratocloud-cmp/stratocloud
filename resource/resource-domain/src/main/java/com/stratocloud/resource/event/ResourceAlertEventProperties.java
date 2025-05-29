package com.stratocloud.resource.event;

import com.stratocloud.resource.ResourceCategory;
import com.stratocloud.resource.ResourceCategoryGroup;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceAlertEventProperties extends ResourceEventProperties{


    public static ResourceAlertEventProperties createExample(){
        ResourceAlertEventProperties properties = new ResourceAlertEventProperties();

        properties.setProviderId("");
        properties.setProviderName("");
        properties.setAccountId(0L);
        properties.setAccountName("");
        properties.setResourceCategory(
                new ResourceCategory(
                        new ResourceCategoryGroup("", ""),
                        "",
                        "",
                        "",
                        0
                )
        );
        properties.setResourceTypeId("");
        properties.setResourceTypeName("");
        properties.setResourceId(0L);
        properties.setResourceName("");
        properties.setResourceOwnerId(0L);
        properties.setResourceTenantId(0L);

        return properties;
    }
}
