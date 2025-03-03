package com.stratocloud.provider.tencent.lb.impl.open.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.lb.impl.open.TencentOpenLoadBalancerHandler;
import com.stratocloud.provider.tencent.zone.TencentZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.clb.v20180317.models.LoadBalancer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentOpenLoadBalancerToZoneHandler implements EssentialRequirementHandler {

    private final TencentOpenLoadBalancerHandler loadBalancerHandler;

    private final TencentZoneHandler zoneHandler;

    public TencentOpenLoadBalancerToZoneHandler(TencentOpenLoadBalancerHandler loadBalancerHandler,
                                                TencentZoneHandler zoneHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_OPEN_LOAD_BALANCER_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "负载均衡与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return loadBalancerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "负载均衡";
    }

    @Override
    public String getRequirementName() {
        return "可用区";
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

        if(Utils.isEmpty(loadBalancer.get().getZones()))
            return List.of();

        Optional<ExternalResource> zone = zoneHandler.describeExternalResource(account, loadBalancer.get().getZones()[0]);
        if(zone.isEmpty())
            return List.of();
        return List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        zone.get(),
                        Map.of()
                )
        );
    }
}
