package com.stratocloud.provider;

import com.stratocloud.provider.dynamic.DynamicResourceHandler;
import com.stratocloud.provider.dynamic.DynamicResourceHandlerLoader;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
public abstract class CachedResourceHandlerLoader implements DynamicResourceHandlerLoader {

    private final Provider provider;

    private final List<DynamicResourceHandler> cachedHandlers = new ArrayList<>();

    public CachedResourceHandlerLoader(Provider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public synchronized List<DynamicResourceHandler> loadResourceHandlers() {
        return cachedHandlers;
    }

    @Override
    public List<RelationshipHandler> loadCapabilitiesByTarget(ResourceHandler targetHandler) {
        List<DynamicResourceHandler> resourceHandlers = loadResourceHandlers();

        List<RelationshipHandler> result = new ArrayList<>();

        for (DynamicResourceHandler resourceHandler : resourceHandlers)
            for (RelationshipHandler requirement : resourceHandler.getRequirements())
                if (requirement.getTarget() == targetHandler)
                    result.add(requirement);

        return result;
    }

    protected abstract List<DynamicResourceHandler> doLoadResourceHandlers();

    private synchronized void replaceResourceHandlers(List<DynamicResourceHandler> resourceHandlers){
        cachedHandlers.clear();
        cachedHandlers.addAll(resourceHandlers);
    }



    @Scheduled(fixedDelay = 15L, timeUnit = TimeUnit.SECONDS)
    public final void refreshResourceHandlers(){
        try {
            replaceResourceHandlers(doLoadResourceHandlers());
        }catch (Exception e){
            log.warn("Cannot refresh dynamic resource handlers in {}: {}." , getClass().getSimpleName(), e.toString());
        }
    }
}
