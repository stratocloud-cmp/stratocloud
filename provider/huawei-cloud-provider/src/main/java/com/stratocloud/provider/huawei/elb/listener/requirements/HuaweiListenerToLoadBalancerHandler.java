package com.stratocloud.provider.huawei.elb.listener.requirements;

import com.huaweicloud.sdk.elb.v3.model.Listener;
import com.huaweicloud.sdk.elb.v3.model.LoadBalancerRef;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.HuaweiLoadBalancerHandler;
import com.stratocloud.provider.huawei.elb.listener.HuaweiListenerHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiListenerToLoadBalancerHandler implements EssentialRequirementHandler {

    private final HuaweiListenerHandler listenerHandler;

    private final HuaweiLoadBalancerHandler loadBalancerHandler;

    public HuaweiListenerToLoadBalancerHandler(HuaweiListenerHandler listenerHandler,
                                               HuaweiLoadBalancerHandler loadBalancerHandler) {
        this.listenerHandler = listenerHandler;
        this.loadBalancerHandler = loadBalancerHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_LISTENER_TO_LOAD_BALANCER_RELATIONSHIP";
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
    public String getCapabilityName() {
        return "监听器";
    }

    @Override
    public String getRequirementName() {
        return "负载均衡实例";
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
        Optional<Listener> listener = listenerHandler.describeListener(account, source.externalId());

        if(listener.isEmpty())
            return List.of();

        List<LoadBalancerRef> loadBalancers = listener.get().getLoadbalancers();

        if(Utils.isEmpty(loadBalancers))
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();
        for (LoadBalancerRef loadBalancerRef : loadBalancers) {
            Optional<ExternalResource> lb = loadBalancerHandler.describeExternalResource(account, loadBalancerRef.getId());
            lb.ifPresent(
                    i -> result.add(
                            new ExternalRequirement(getRelationshipTypeId(), lb.get(), Map.of())
                    )
            );
        }

        return result;
    }
}
