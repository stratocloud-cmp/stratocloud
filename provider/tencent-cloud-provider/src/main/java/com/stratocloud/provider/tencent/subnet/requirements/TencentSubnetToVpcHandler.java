package com.stratocloud.provider.tencent.subnet.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.subnet.TencentSubnetHandler;
import com.stratocloud.provider.tencent.vpc.TencentVpcHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.vpc.v20170312.models.Subnet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentSubnetToVpcHandler implements EssentialRequirementHandler {

    private final TencentSubnetHandler subnetHandler;

    private final TencentVpcHandler vpcHandler;

    public TencentSubnetToVpcHandler(TencentSubnetHandler subnetHandler,
                                     TencentVpcHandler vpcHandler) {
        this.subnetHandler = subnetHandler;
        this.vpcHandler = vpcHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_SUBNET_TO_VPC_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "子网与私有网络";
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
        Optional<Subnet> subnet = subnetHandler.describeSubnet(account, source.externalId());

        if(subnet.isEmpty())
            return List.of();

        Optional<ExternalResource> vpc = vpcHandler.describeExternalResource(account, subnet.get().getVpcId());

        if(vpc.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                vpc.get(),
                Map.of()
        ));
    }

}
