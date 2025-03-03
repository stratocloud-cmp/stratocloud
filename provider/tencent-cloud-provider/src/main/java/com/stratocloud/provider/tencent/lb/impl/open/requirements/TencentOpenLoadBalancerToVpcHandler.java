package com.stratocloud.provider.tencent.lb.impl.open.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.lb.impl.open.TencentOpenLoadBalancerHandler;
import com.stratocloud.provider.tencent.vpc.TencentVpcHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.clb.v20180317.models.LoadBalancer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentOpenLoadBalancerToVpcHandler implements EssentialRequirementHandler {

    private final TencentOpenLoadBalancerHandler loadBalancerHandler;

    private final TencentVpcHandler vpcHandler;

    public TencentOpenLoadBalancerToVpcHandler(TencentOpenLoadBalancerHandler loadBalancerHandler,
                                               TencentVpcHandler vpcHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
        this.vpcHandler = vpcHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_OPEN_LOAD_BALANCER_TO_VPC_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "负载均衡与私有网络";
    }

    @Override
    public ResourceHandler getSource() {
        return loadBalancerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return vpcHandler;
    }

    @Override
    public String getCapabilityName() {
        return "负载均衡";
    }

    @Override
    public String getRequirementName() {
        return "私有网络";
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
        Optional<ExternalResource> vpc = vpcHandler.describeExternalResource(account, loadBalancer.get().getVpcId());
        if(vpc.isEmpty())
            return List.of();
        return List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        vpc.get(),
                        Map.of()
                )
        );
    }
}
