package com.stratocloud.provider;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.relationship.DependsOnRelationshipHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.utils.ContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.*;
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

        ApplicationContext applicationContext = ContextUtil.getApplicationContext();

        var relationshipHandlers = applicationContext.getBeansOfType(RelationshipHandler.class).values();

        return relationshipHandlers.stream().filter(
                r-> Objects.equals(r.getRelationshipTypeId(), relationshipTypeId)
        ).findAny().orElseThrow(
                () -> new StratoException("Relationship handler not found by id %s.".formatted(relationshipTypeId))
        );
    }
}
