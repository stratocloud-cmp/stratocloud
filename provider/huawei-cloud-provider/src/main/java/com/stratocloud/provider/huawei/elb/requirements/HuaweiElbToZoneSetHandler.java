package com.stratocloud.provider.huawei.elb.requirements;

import com.huaweicloud.sdk.elb.v3.model.LoadBalancer;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.HuaweiLoadBalancerHandler;
import com.stratocloud.provider.huawei.elb.zone.HuaweiElbZoneSet;
import com.stratocloud.provider.huawei.elb.zone.HuaweiElbZoneSetHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbToZoneSetHandler implements EssentialRequirementHandler {

    private final HuaweiLoadBalancerHandler loadBalancerHandler;

    private final HuaweiElbZoneSetHandler zoneSetHandler;

    public HuaweiElbToZoneSetHandler(HuaweiLoadBalancerHandler loadBalancerHandler,
                                     HuaweiElbZoneSetHandler zoneSetHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
        this.zoneSetHandler = zoneSetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_ELB_TO_ZONE_SET_HANDLER";
    }

    @Override
    public String getRelationshipTypeName() {
        return "负载均衡与可用区集合";
    }

    @Override
    public ResourceHandler getSource() {
        return loadBalancerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneSetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "负载均衡";
    }

    @Override
    public String getRequirementName() {
        return "可用区集合";
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
        Optional<LoadBalancer> elb = loadBalancerHandler.describeLoadBalancer(account, source.externalId());

        if(elb.isEmpty())
            return List.of();

        List<String> zoneCodes = elb.get().getAvailabilityZoneList();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) loadBalancerHandler.getProvider();
        List<HuaweiElbZoneSet> zoneSets = provider.buildClient(account).elb().describeZoneSets();

        return zoneSets.stream().filter(
                set -> set.containsAll(zoneCodes)
        ).findAny().map(
                set -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                zoneSetHandler.toExternalResource(account, set),
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
