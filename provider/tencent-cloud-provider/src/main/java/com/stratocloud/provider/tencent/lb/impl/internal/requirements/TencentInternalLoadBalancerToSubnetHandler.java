package com.stratocloud.provider.tencent.lb.impl.internal.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.lb.impl.internal.TencentInternalLoadBalancerHandler;
import com.stratocloud.provider.tencent.subnet.TencentSubnetHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.clb.v20180317.models.LoadBalancer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentInternalLoadBalancerToSubnetHandler implements EssentialRequirementHandler {

    private final TencentInternalLoadBalancerHandler loadBalancerHandler;

    private final TencentSubnetHandler subnetHandler;

    public TencentInternalLoadBalancerToSubnetHandler(TencentInternalLoadBalancerHandler loadBalancerHandler,
                                                      TencentSubnetHandler subnetHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_INTERNAL_LOAD_BALANCER_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "负载均衡与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return loadBalancerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return subnetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "负载均衡";
    }

    @Override
    public String getRequirementName() {
        return "子网";
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
        Optional<LoadBalancer> loadBalancer = loadBalancerHandler.describeLoadBalancer(account, source.externalId());
        if(loadBalancer.isEmpty())
            return List.of();
        Optional<ExternalResource> subnet = subnetHandler.describeExternalResource(account, loadBalancer.get().getSubnetId());
        return subnet.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
