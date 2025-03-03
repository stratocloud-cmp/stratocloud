package com.stratocloud.provider.script.init.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.script.init.InitScriptHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;

import java.util.List;

public class InitScriptToGuestOsHandler implements ExclusiveRequirementHandler {

    private final InitScriptHandler initScriptHandler;

    private final GuestOsHandler guestOsHandler;


    public InitScriptToGuestOsHandler(InitScriptHandler initScriptHandler,
                                      GuestOsHandler guestOsHandler) {
        this.initScriptHandler = initScriptHandler;
        this.guestOsHandler = guestOsHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "%s_TO_%s_RELATIONSHIP".formatted(
                initScriptHandler.getResourceTypeId(),
                guestOsHandler.getResourceTypeId()
        );
    }

    @Override
    public String getRelationshipTypeName() {
        return "%s与%s".formatted(guestOsHandler.getResourceTypeName(), initScriptHandler.getResourceTypeName());
    }

    @Override
    public ResourceHandler getSource() {
        return initScriptHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return guestOsHandler;
    }

    @Override
    public String getCapabilityName() {
        return initScriptHandler.getResourceTypeName();
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
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public boolean visibleInTarget() {
        return initScriptHandler.getDefinition().isVisibleInTarget();
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        return List.of();
    }
}
