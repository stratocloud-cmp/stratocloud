package com.stratocloud.provider.script.software;

import com.stratocloud.provider.CachedResourceHandlerLoader;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.dynamic.DynamicResourceHandler;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.script.software.requirements.ExclusiveSoftwareRequirementHandler;
import com.stratocloud.provider.script.software.requirements.SoftwareRelationshipHandler;
import com.stratocloud.provider.script.software.requirements.SoftwareToGuestOsHandler;
import com.stratocloud.repository.SoftwareDefinitionRepository;
import com.stratocloud.script.SoftwareDefinition;
import com.stratocloud.script.SoftwareRequirement;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class SoftwareHandlerLoader extends CachedResourceHandlerLoader {

    public SoftwareHandlerLoader(Provider provider) {
        super(provider);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<DynamicResourceHandler> doLoadResourceHandlers() {
        return (List<DynamicResourceHandler>) loadSoftwareHandlers();
    }

    private List<? extends DynamicResourceHandler> loadSoftwareHandlers(){
        var softwareDefinitionRepository = ContextUtil.getBean(SoftwareDefinitionRepository.class);

        List<SoftwareDefinition> definitions = softwareDefinitionRepository.findAllEnabled();

        if(Utils.isEmpty(definitions))
            return List.of();

        var softwareHandlers = definitions.stream().map(this::toSoftwareHandler).toList();

        Map<String, SoftwareHandler> resourceTypeIdMappingHandlers = softwareHandlers.stream().collect(
                Collectors.toMap(
                        DynamicResourceHandler::getResourceTypeId,
                        rh -> rh
                )
        );

        for (SoftwareDefinition definition : definitions) {
            if(Utils.isEmpty(definition.getRequirements()))
                continue;

            for (SoftwareRequirement requirement : definition.getRequirements()) {
                String sourceTypeId = requirement.getSource().generateSoftwareResourceTypeId(
                        getProvider().getId()
                );
                String targetTypeId = requirement.getTarget().generateSoftwareResourceTypeId(
                        getProvider().getId()
                );

                SoftwareHandler sourceHandler = resourceTypeIdMappingHandlers.get(sourceTypeId);
                SoftwareHandler targetHandler = resourceTypeIdMappingHandlers.get(targetTypeId);

                if(sourceHandler == null || targetHandler == null){
                    log.warn("Cannot register software requirement {}.", requirement.getRequirementKey());
                    continue;
                }

                SoftwareRelationshipHandler relationshipHandler;

                if(requirement.isExclusive())
                    relationshipHandler = new ExclusiveSoftwareRequirementHandler(
                            sourceHandler, targetHandler, requirement
                    );
                else
                    relationshipHandler = new SoftwareRelationshipHandler(
                            sourceHandler, targetHandler, requirement
                    );

                sourceHandler.registerRequirement(relationshipHandler);
                targetHandler.registerCapability(relationshipHandler);
            }
        }

        return softwareHandlers;
    }

    private SoftwareHandler toSoftwareHandler(SoftwareDefinition softwareDefinition) {
        SoftwareHandler softwareHandler = new SoftwareHandler(getProvider(), softwareDefinition);

        for (ResourceHandler resourceHandler : getProvider().getResourceHandlers()) {
            if(resourceHandler instanceof GuestOsHandler guestOsHandler){
                var softwareToGuestOsHandler = new SoftwareToGuestOsHandler(softwareHandler, guestOsHandler);
                softwareHandler.registerRequirement(softwareToGuestOsHandler);
            }
        }

        return softwareHandler;
    }
}
