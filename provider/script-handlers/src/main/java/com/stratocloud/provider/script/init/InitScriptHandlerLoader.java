package com.stratocloud.provider.script.init;

import com.stratocloud.provider.CachedResourceHandlerLoader;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.dynamic.DynamicResourceHandler;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.script.init.requirements.InitScriptToGuestOsHandler;
import com.stratocloud.repository.ScriptDefinitionRepository;
import com.stratocloud.script.ScriptDefinition;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.Utils;

import java.util.List;


public class InitScriptHandlerLoader extends CachedResourceHandlerLoader {

    public InitScriptHandlerLoader(Provider provider) {
        super(provider);
    }

    @Override
    protected List<DynamicResourceHandler> doLoadResourceHandlers() {
        ScriptDefinitionRepository scriptDefinitionRepository = ContextUtil.getBean(ScriptDefinitionRepository.class);

        List<ScriptDefinition> definitions = scriptDefinitionRepository.findAllEnabled();

        if(Utils.isEmpty(definitions))
            return List.of();

        return definitions.stream().map(this::toInitScriptHandler).toList();
    }

    private DynamicResourceHandler toInitScriptHandler(ScriptDefinition scriptDefinition) {
        InitScriptHandler initScriptHandler = new InitScriptHandler(getProvider(), scriptDefinition);

        for (ResourceHandler resourceHandler : getProvider().getResourceHandlers()) {
            if(resourceHandler instanceof GuestOsHandler guestOsHandler){
                var initScriptToGuestOsHandler = new InitScriptToGuestOsHandler(initScriptHandler, guestOsHandler);
                initScriptHandler.registerRequirement(initScriptToGuestOsHandler);
            }
        }

        return initScriptHandler;
    }
}
