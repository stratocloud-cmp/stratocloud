package com.stratocloud.provider.tencent.lb.common;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.lb.TencentLoadBalancerHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentListener;
import com.stratocloud.provider.tencent.lb.listener.TencentListenerId;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class TencentListenerToLbHandler implements ExclusiveRequirementHandler {

    private final TencentLoadBalancerHandler loadBalancerHandler;

    private final TencentListenerHandler listenerHandler;

    protected TencentListenerToLbHandler(TencentLoadBalancerHandler loadBalancerHandler,
                                         TencentListenerHandler listenerHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
        this.listenerHandler = listenerHandler;
    }

    @Override
    public String getRelationshipTypeName() {
        return "负载均衡与监听器";
    }

    @Override
    public ResourceHandler getSource() {
        return listenerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return loadBalancerHandler;
    }



    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<TencentListener> listener = listenerHandler.describeListener(account, source.externalId());

        if(listener.isEmpty())
            return List.of();

        String lbId = TencentListenerId.fromString(source.externalId()).lbId();

        Optional<ExternalResource> loadBalancer = loadBalancerHandler.describeExternalResource(account, lbId);

        if(loadBalancer.isEmpty())
            return List.of();

        return List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        loadBalancer.get(),
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
