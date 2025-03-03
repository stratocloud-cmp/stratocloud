package com.stratocloud.provider.script.init;


import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.dynamic.DynamicResourceHandler;
import com.stratocloud.provider.script.init.actions.InitScriptBuildHandler;
import com.stratocloud.resource.*;
import com.stratocloud.script.ScriptDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InitScriptHandler extends AbstractResourceHandler implements DynamicResourceHandler {

    private final Provider provider;

    private final ScriptDefinition definition;

    public InitScriptHandler(Provider provider, ScriptDefinition scriptDefinition) {
        this.provider = provider;
        this.definition = scriptDefinition;

        InitScriptBuildHandler buildHandler = new InitScriptBuildHandler(this);

        registerActionHandler(buildHandler);
    }

    public ScriptDefinition getDefinition() {
        return definition;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return provider.getId()+"_"+getResourceCategory().id()+"_"+definition.getDefinitionKey();
    }

    @Override
    public String getResourceTypeName() {
        return definition.getName();
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.INIT_SCRIPT;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return Optional.of(
                new ExternalResource(
                        provider.getId(),
                        account.getId(),
                        getResourceCategory().id(),
                        getResourceTypeId(),
                        externalId,
                        externalId,
                        ResourceState.UNKNOWN
                )
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return List.of();
    }

    @Override
    public void synchronize(Resource resource) {

    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
