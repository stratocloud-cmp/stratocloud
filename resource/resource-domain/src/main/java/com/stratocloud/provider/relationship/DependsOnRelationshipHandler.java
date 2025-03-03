package com.stratocloud.provider.relationship;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;

import java.util.List;

@SuppressWarnings("ClassCanBeRecord")
public class DependsOnRelationshipHandler implements RelationshipHandler{

    public static final String TYPE_ID = "DEPENDS_ON";
    private final ResourceHandler source;

    private final ResourceHandler target;

    public DependsOnRelationshipHandler(ResourceHandler source, ResourceHandler target) {
        this.source = source;
        this.target = target;
    }


    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "依赖于";
    }

    @Override
    public ResourceHandler getSource() {
        return source;
    }

    @Override
    public ResourceHandler getTarget() {
        return target;
    }

    @Override
    public String getCapabilityName() {
        return source.getResourceCategory().name() + "(被依赖)";
    }

    @Override
    public String getRequirementName() {
        return target.getResourceCategory().name() + "(依赖)";
    }

    @Override
    public String getConnectActionName() {
        return "建立依赖";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除依赖";
    }

    @Override
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        return List.of();
    }


    @Override
    public RelationshipActionResult checkConnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }
}
