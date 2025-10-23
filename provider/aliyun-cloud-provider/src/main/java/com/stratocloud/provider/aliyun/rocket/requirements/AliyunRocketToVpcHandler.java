package com.stratocloud.provider.aliyun.rocket.requirements;

import com.aliyun.rocketmq20220801.models.GetInstanceResponseBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.rocket.AliyunRocketHandler;
import com.stratocloud.provider.aliyun.rocket.RocketInstance;
import com.stratocloud.provider.aliyun.vpc.AliyunVpcHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunRocketToVpcHandler implements EssentialRequirementHandler {

    private final AliyunRocketHandler rocketHandler;

    private final AliyunVpcHandler vpcHandler;

    public AliyunRocketToVpcHandler(AliyunRocketHandler rocketHandler, AliyunVpcHandler vpcHandler) {
        this.rocketHandler = rocketHandler;
        this.vpcHandler = vpcHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_ROCKET_TO_VPC_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云RocketMQ实例与VPC";
    }

    @Override
    public ResourceHandler getSource() {
        return rocketHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return vpcHandler;
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }

    @Override
    public String getCapabilityName() {
        return "RocketMQ实例";
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
        Optional<RocketInstance> rocketInstance = rocketHandler.describeInstance(account, source.externalId());

        if(rocketInstance.isEmpty())
            return List.of();

        AliyunCloudProvider provider = (AliyunCloudProvider) rocketHandler.getProvider();
        AliyunClient client = provider.buildClient(account);
        var detail = client.rocket().describeInstanceDetail(source.externalId());

        GetInstanceResponseBody.GetInstanceResponseBodyDataNetworkInfo networkInfo = detail.getNetworkInfo();
        if(networkInfo==null || networkInfo.getVpcInfo()==null || Utils.isBlank(networkInfo.getVpcInfo().getVpcId()))
            return List.of();

        Optional<ExternalResource> vpcResource = vpcHandler.describeExternalResource(
                account,
                networkInfo.getVpcInfo().getVpcId()
        );

        return vpcResource.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
