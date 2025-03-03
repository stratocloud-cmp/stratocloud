package com.stratocloud.provider.huawei.elb.requirements;

import com.huaweicloud.sdk.elb.v3.model.LoadBalancer;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.HuaweiLoadBalancerHandler;
import com.stratocloud.provider.huawei.subnet.HuaweiSubnetHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbToFrontendSubnetHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "HUAWEI_LB_TO_FRONTEND_SUBNET_RELATIONSHIP";
    private final HuaweiLoadBalancerHandler loadBalancerHandler;

    private final HuaweiSubnetHandler subnetHandler;

    public HuaweiElbToFrontendSubnetHandler(HuaweiLoadBalancerHandler loadBalancerHandler,
                                            HuaweiSubnetHandler subnetHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
        this.subnetHandler = subnetHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
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
        return "负载均衡(前端子网)";
    }

    @Override
    public String getRequirementName() {
        return "前端子网";
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
        Optional<LoadBalancer> lb = loadBalancerHandler.describeLoadBalancer(account, source.externalId());

        if(lb.isEmpty())
            return List.of();

        String neutronSubnetId = lb.get().getVipSubnetCidrId();

        var subnet = subnetHandler.describeSubnetByNeutronSubnetId(account, neutronSubnetId);

        return subnet.map(s -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        subnetHandler.toExternalResource(account, s),
                        Map.of()
                )
        )).orElseGet(List::of);

    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
