package com.stratocloud.provider.script.software.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.script.software.SoftwareHandler;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.ContextUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SoftwareToGuestOsHandler implements EssentialRequirementHandler {

    private final SoftwareHandler softwareHandler;

    private final GuestOsHandler guestOsHandler;

    public SoftwareToGuestOsHandler(SoftwareHandler softwareHandler,
                                    GuestOsHandler guestOsHandler) {
        this.softwareHandler = softwareHandler;
        this.guestOsHandler = guestOsHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "%s_TO_%s_RELATIONSHIP".formatted(
                softwareHandler.getResourceTypeId(),
                guestOsHandler.getResourceTypeId()
        );
    }

    @Override
    public String getRelationshipTypeName() {
        return "%s与%s".formatted(guestOsHandler.getResourceTypeName(), softwareHandler.getResourceTypeName());
    }

    @Override
    public ResourceHandler getSource() {
        return softwareHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return guestOsHandler;
    }

    @Override
    public String getCapabilityName() {
        return softwareHandler.getResourceTypeName();
    }

    @Override
    public String getRequirementName() {
        return guestOsHandler.getResourceTypeName();
    }

    @Override
    public String getConnectActionName() {
        return "关联";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }


    @Override
    public boolean visibleInTarget() {
        return softwareHandler.getDefinition().isVisibleInTarget();
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        ResourceRepository resourceRepository = ContextUtil.getBean(ResourceRepository.class);

        Optional<Resource> software = resourceRepository.findByExternalResource(source);

        if(software.isEmpty())
            return List.of();

        Optional<Resource> guestOs = software.get().getEssentialTargetByType(getRelationshipTypeId());

        if(guestOs.isEmpty())
            return List.of();

        Optional<ExternalResource> externalGuestOs = guestOs.get().toExternalResource();

        return externalGuestOs.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
