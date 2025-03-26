package com.stratocloud.provider;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.relationship.DependsOnRelationshipHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ProviderRegistry {
    private static final Map<String, Provider> providerMap = new ConcurrentHashMap<>();

    public static void register(Provider provider){
        String providerId = provider.getId();
        if(providerMap.containsKey(providerId))
            throw new StratoException("Cannot register 2 providers with same providerId: %s.".formatted(providerId));

        providerMap.put(providerId, provider);
    }

    public static Provider getProvider(String providerId){
        Provider provider = providerMap.get(providerId);

        if(provider == null)
            throw new StratoException("Provider not found by id: %s.".formatted(providerId));

        return provider;
    }

    public static ResourceHandler getResourceHandler(String resourceTypeId) {
        for (Provider provider : providerMap.values()) {
            if(provider.hasResourceType(resourceTypeId))
                return provider.getResourceHandlerByType(resourceTypeId);
        }
        throw new StratoException("ResourceHandler not found by id: %s.".formatted(resourceTypeId));
    }

    public static List<Provider> getProviders(){
        return new ArrayList<>(providerMap.values());
    }

    public static RelationshipHandler getRelationshipHandler(String relationshipTypeId) {
        if(DependsOnRelationshipHandler.TYPE_ID.equals(relationshipTypeId))
            return new DependsOnRelationshipHandler(null, null);

        for (Provider provider : getProviders()) {
            for (ResourceHandler resourceHandler : provider.getResourceHandlers()) {
                var relationshipHandler = resourceHandler.getRequirements().stream().filter(
                        rel -> Objects.equals(
                                rel.getRelationshipTypeId(),
                                relationshipTypeId
                        )
                ).findAny();

                if(relationshipHandler.isPresent())
                    return relationshipHandler.get();
            }
        }

        throw new StratoException("Relationship handler not found by id %s.".formatted(relationshipTypeId));
    }
}
