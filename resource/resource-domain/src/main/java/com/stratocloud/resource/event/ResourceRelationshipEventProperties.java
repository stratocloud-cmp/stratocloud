package com.stratocloud.resource.event;
import com.stratocloud.resource.ResourceCategory;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.resource.*;
import com.stratocloud.utils.SecurityUtil;
import com.stratocloud.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class ResourceRelationshipEventProperties extends ResourceEventProperties {
    private String relationshipTypeId;
    private String relationshipTypeName;
    private boolean connectAction;
    private Map<String, Object> relationshipInputs;

    public static ResourceRelationshipEventProperties create(Relationship relationship,
                                                             ExternalAccount account,
                                                             boolean connectAction){
        Resource source = relationship.getSource();

        ResourceRelationshipEventProperties properties = new ResourceRelationshipEventProperties();

        properties.setProviderId(account.getProviderId());
        properties.setProviderName(account.getProvider().getName());

        properties.setAccountId(account.getId());
        properties.setAccountName(account.getName());

        properties.setResourceCategory(source.getResourceHandler().getResourceCategory());
        properties.setResourceTypeId(source.getResourceHandler().getResourceTypeId());
        properties.setResourceTypeName(source.getResourceHandler().getResourceTypeName());

        properties.setResourceId(source.getId());
        properties.setResourceName(source.getName());

        properties.setResourceOwnerId(source.getOwnerId());
        properties.setResourceTenantId(source.getTenantId());

        properties.setConnectAction(connectAction);
        properties.setRelationshipTypeId(relationship.getType());
        properties.setRelationshipTypeName(relationship.getTypeName());
        properties.setRelationshipInputs(eraseSensitiveInfo(relationship.getProperties()));

        return properties;
    }

    private static Map<String, Object> eraseSensitiveInfo(Map<String, Object> relationshipInputs) {
        Map<String, Object> result = new HashMap<>();

        if(Utils.isEmpty(relationshipInputs))
            return result;

        Set<String> keys = relationshipInputs.keySet();

        for (String key : keys) {
            if(SecurityUtil.isSensitiveProperty(key))
                continue;

            result.put(key, relationshipInputs.get(key));
        }

        return result;
    }

    public static ResourceRelationshipEventProperties createExample(){
        ResourceRelationshipEventProperties properties = new ResourceRelationshipEventProperties();

        properties.setRelationshipTypeId("");
        properties.setRelationshipTypeName("");
        properties.setConnectAction(false);
        properties.setRelationshipInputs(Map.of());
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
