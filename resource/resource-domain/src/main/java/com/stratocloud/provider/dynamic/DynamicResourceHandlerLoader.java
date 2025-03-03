package com.stratocloud.provider.dynamic;

import com.stratocloud.provider.Provider;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;

import java.util.List;

public interface DynamicResourceHandlerLoader {

    Provider getProvider();

    List<DynamicResourceHandler> loadResourceHandlers();


    List<RelationshipHandler> loadCapabilitiesByTarget(ResourceHandler targetHandler);
}
