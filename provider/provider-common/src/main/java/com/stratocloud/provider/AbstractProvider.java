package com.stratocloud.provider;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.dynamic.DynamicResourceHandler;
import com.stratocloud.provider.dynamic.DynamicResourceHandlerLoader;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.repository.ExternalAccountRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class AbstractProvider implements Provider {

    private final List<ResourceHandler> resourceHandlers = new ArrayList<>();

    private final List<DynamicResourceHandlerLoader> loaders = new ArrayList<>();

    private final ExternalAccountRepository accountRepository;

    protected AbstractProvider(ExternalAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    private List<DynamicResourceHandler> loadDynamicResourceHandlers(){
        List<DynamicResourceHandler> result = new ArrayList<>();

        for (DynamicResourceHandlerLoader loader : loaders) {
            try {
                result.addAll(loader.loadResourceHandlers());
            }catch (Exception e){
                log.warn("Failed to load resource handlers from {}: {}",
                        loader.getClass().getSimpleName(), e.toString());
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<ResourceHandler> getResourceHandlers() {
        List<ResourceHandler> result = new ArrayList<>();
        result.addAll(resourceHandlers);
        result.addAll(loadDynamicResourceHandlers());
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<DynamicResourceHandlerLoader> getResourceHandlerLoaders() {
        return loaders;
    }

    @Override
    public ResourceHandler getResourceHandlerByType(String resourceTypeId){
        return getResourceHandlers().stream().filter(
                rh -> rh.getResourceTypeId().equals(resourceTypeId)
        ).findAny().orElseThrow(()->new StratoException("Resource handler not found: "+resourceTypeId));
    }

    @Override
    public Optional<? extends ResourceHandler> getResourceHandlerByCategory(String resourceCategoryId){
        return getResourceHandlers().stream().filter(
                rh -> rh.getResourceCategory().id().equals(resourceCategoryId)
        ).findAny();
    }

    @Override
    public boolean hasResourceType(String resourceTypeId){
        return getResourceHandlers().stream().anyMatch(
                rh -> rh.getResourceTypeId().equals(resourceTypeId)
        );
    }

    @Override
    public synchronized void register(ResourceHandler resourceHandler) {
        resourceHandlers.add(resourceHandler);
    }

    @Override
    public synchronized void registerLoader(DynamicResourceHandlerLoader loader) {
        loaders.add(loader);
    }

    @Override
    public List<ResourceHandler> getManagementRoots() {
        return getResourceHandlers().stream().filter(rh -> rh.isManageable() && rh.getRequirements().isEmpty()).toList();
    }

    @Override
    public ExternalAccountRepository getAccountRepository() {
        return accountRepository;
    }
}
