package com.stratocloud.resource.event;
import com.stratocloud.resource.ResourceCategory;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceAction;
import com.stratocloud.resource.ResourceCategoryGroup;
import com.stratocloud.utils.SecurityUtil;
import com.stratocloud.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class ResourceActionEventProperties extends ResourceEventProperties {
    private ResourceAction resourceAction;
    private Map<String, Object> actionInputs;

    public static ResourceActionEventProperties create(Resource resource,
                                                       ExternalAccount account,
                                                       ResourceAction action,
                                                       Map<String, Object> actionInputs){
        ResourceActionEventProperties properties = new ResourceActionEventProperties();

        properties.setProviderId(account.getProviderId());
        properties.setProviderName(account.getProvider().getName());

        properties.setAccountId(account.getId());
        properties.setAccountName(account.getName());

        properties.setResourceCategory(resource.getResourceHandler().getResourceCategory());
        properties.setResourceTypeId(resource.getResourceHandler().getResourceTypeId());
        properties.setResourceTypeName(resource.getResourceHandler().getResourceTypeName());

        properties.setResourceId(resource.getId());
        properties.setResourceName(resource.getName());

        properties.setResourceOwnerId(resource.getOwnerId());
        properties.setResourceTenantId(resource.getTenantId());

        properties.setResourceAction(action);
        properties.setActionInputs(eraseSensitiveInfo(actionInputs));

        return properties;
    }

    private static Map<String, Object> eraseSensitiveInfo(Map<String, Object> actionInputs) {
        Map<String, Object> result = new HashMap<>();

        if(Utils.isEmpty(actionInputs))
            return result;

        Set<String> keys = actionInputs.keySet();

        for (String key : keys) {
            if(SecurityUtil.isSensitiveProperty(key))
                continue;

            result.put(key, actionInputs.get(key));
        }

        return result;
    }

    public static ResourceActionEventProperties createExample(){
        ResourceActionEventProperties properties = new ResourceActionEventProperties();

        properties.setResourceAction(new ResourceAction("", "", 0));
        properties.setActionInputs(Map.of());
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
