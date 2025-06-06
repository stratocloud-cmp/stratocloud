package com.stratocloud.provider.huawei.elb.requirements;

import com.huaweicloud.sdk.elb.v3.model.LoadBalancer;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.HuaweiLoadBalancerHandler;
import com.stratocloud.provider.huawei.elb.flavor.HuaweiElbL7FlavorHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbToL7FlavorHandler implements ExclusiveRequirementHandler {

    public static final String TYPE_ID = "HUAWEI_ELB_TO_L7_FLAVOR_RELATIONSHIP";
    private final HuaweiLoadBalancerHandler loadBalancerHandler;

    private final HuaweiElbL7FlavorHandler flavorHandler;

    public HuaweiElbToL7FlavorHandler(HuaweiLoadBalancerHandler loadBalancerHandler,
                                      HuaweiElbL7FlavorHandler flavorHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
        this.flavorHandler = flavorHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "ELB与应用型规格";
    }

    @Override
    public ResourceHandler getSource() {
        return loadBalancerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return flavorHandler;
    }

    @Override
    public String getCapabilityName() {
        return "负载均衡器";
    }

    @Override
    public String getRequirementName() {
        return "应用型规格";
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
        Optional<LoadBalancer> elb = loadBalancerHandler.describeLoadBalancer(account, source.externalId());

        if(elb.isEmpty())
            return List.of();

        String l7FlavorId = elb.get().getL7FlavorId();

        Optional<ExternalResource> flavor = flavorHandler.describeExternalResource(account, l7FlavorId);

        return flavor.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
