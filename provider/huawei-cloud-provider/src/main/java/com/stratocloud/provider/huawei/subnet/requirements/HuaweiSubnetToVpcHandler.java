package com.stratocloud.provider.huawei.subnet.requirements;

import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.subnet.HuaweiSubnetHandler;
import com.stratocloud.provider.huawei.vpc.HuaweiVpcHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiSubnetToVpcHandler implements EssentialRequirementHandler {

    private final HuaweiSubnetHandler subnetHandler;

    private final HuaweiVpcHandler vpcHandler;

    public HuaweiSubnetToVpcHandler(HuaweiSubnetHandler subnetHandler, HuaweiVpcHandler vpcHandler) {
        this.subnetHandler = subnetHandler;
        this.vpcHandler = vpcHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_SUBNET_TO_VPC_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云VPC与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return subnetHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return vpcHandler;
    }

    @Override
    public String getCapabilityName() {
        return "子网";
    }

    @Override
    public String getRequirementName() {
        return "VPC";
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
        Optional<Subnet> subnet = subnetHandler.describeSubnet(account, source.externalId());

        if(subnet.isEmpty())
            return List.of();

        String vpcId = subnet.get().getVpcId();

        Optional<ExternalResource> vpc = vpcHandler.describeExternalResource(account, vpcId);

        return vpc.map(v -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        v,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
