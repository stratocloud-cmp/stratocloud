package com.stratocloud.provider;


import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.dynamic.DynamicResourceHandlerLoader;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.repository.ExternalAccountRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Provider {
    String getId();

    String getName();

    Class<? extends ExternalAccountProperties> getExternalAccountPropertiesClass();

    void validateConnection(ExternalAccount externalAccount);

    void register(ResourceHandler resourceHandler);

    void registerLoader(DynamicResourceHandlerLoader loader);

    List<? extends ResourceHandler> getResourceHandlers();

    List<DynamicResourceHandlerLoader> getResourceHandlerLoaders();


    ResourceHandler getResourceHandlerByType(String resourceTypeId);

    Optional<? extends ResourceHandler> getResourceHandlerByCategory(String resourceCategoryId);

    boolean hasResourceType(String resourceTypeId);

    List<ResourceHandler> getManagementRoots();

    void eraseSensitiveInfo(Map<String, Object> properties);

    ExternalAccountRepository getAccountRepository();


    default Float getBalance(ExternalAccount account){
        return 0.0f;
    }


    default Optional<MetricsProvider> getMetricsProvider(){
        return Optional.empty();
    }
}
