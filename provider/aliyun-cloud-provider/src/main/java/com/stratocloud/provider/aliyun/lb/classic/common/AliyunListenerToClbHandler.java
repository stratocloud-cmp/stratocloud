package com.stratocloud.provider.aliyun.lb.classic.common;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.lb.classic.AliyunClbHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AliyunListenerToClbHandler implements ExclusiveRequirementHandler {

    protected final AliyunListenerHandler listenerHandler;

    protected final AliyunClbHandler clbHandler;

    protected AliyunListenerToClbHandler(AliyunListenerHandler listenerHandler,
                                         AliyunClbHandler clbHandler) {
        this.listenerHandler = listenerHandler;
        this.clbHandler = clbHandler;
    }


    @Override
    public ResourceHandler getSource() {
        return listenerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return clbHandler;
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<AliyunListener> listener = listenerHandler.describeListener(account, source.externalId());

        if(listener.isEmpty())
            return List.of();

        String loadBalancerId = listener.get().listenerId().loadBalancerId();

        Optional<ExternalResource> clb = clbHandler.describeExternalResource(account, loadBalancerId);

        if(clb.isEmpty())
            return List.of();

        return List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        clb.get(),
                        Map.of()
                )
        );
    }

    @Override
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }
}
