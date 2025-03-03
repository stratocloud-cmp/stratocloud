package com.stratocloud.provider.huawei.elb.requirements;

import com.huaweicloud.sdk.elb.v3.model.LoadBalancer;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.HuaweiLoadBalancerHandler;
import com.stratocloud.provider.huawei.subnet.HuaweiSubnetHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbToBackendSubnetHandler implements RelationshipHandler {

    public static final String TYPE_ID = "HUAWEI_LB_TO_BACKEND_SUBNET_RELATIONSHIP";
    private final HuaweiLoadBalancerHandler loadBalancerHandler;

    private final HuaweiSubnetHandler subnetHandler;

    public HuaweiElbToBackendSubnetHandler(HuaweiLoadBalancerHandler loadBalancerHandler,
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
        return "负载均衡与后端子网";
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
        return "负载均衡(后端子网)";
    }

    @Override
    public String getRequirementName() {
        return "后端子网";
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
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<LoadBalancer> lb = loadBalancerHandler.describeLoadBalancer(account, source.externalId());

        if(lb.isEmpty())
            return List.of();

        List<String> neutronNetworkIds = lb.get().getElbVirsubnetIds();

        if(Utils.isEmpty(neutronNetworkIds))
            return List.of();

        List<Subnet> subnets = subnetHandler.describeSubnetsByNeutronNetworkIds(account, neutronNetworkIds);

        return subnets.stream().map(
                s -> new ExternalRequirement(
                        getRelationshipTypeId(),
                        subnetHandler.toExternalResource(account, s),
                        Map.of()
                )
        ).toList();
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
