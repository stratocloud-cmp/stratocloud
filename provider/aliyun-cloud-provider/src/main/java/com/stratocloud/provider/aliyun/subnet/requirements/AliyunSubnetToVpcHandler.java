package com.stratocloud.provider.aliyun.subnet.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnet;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnetHandler;
import com.stratocloud.provider.aliyun.vpc.AliyunVpcHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunSubnetToVpcHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "ALIYUN_SUBNET_TO_VPC_RELATIONSHIP";
    private final AliyunSubnetHandler subnetHandler;

    private final AliyunVpcHandler vpcHandler;

    public AliyunSubnetToVpcHandler(AliyunSubnetHandler subnetHandler,
                                    AliyunVpcHandler vpcHandler) {
        this.subnetHandler = subnetHandler;
        this.vpcHandler = vpcHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
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
        Optional<AliyunSubnet> subnet = subnetHandler.describeSubnet(account, source.externalId());

        if(subnet.isEmpty())
            return List.of();

        Optional<ExternalResource> vpc = vpcHandler.describeExternalResource(account, subnet.get().detail().getVpcId());

        if(vpc.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                vpc.get(),
                Map.of()
        ));
    }

}
