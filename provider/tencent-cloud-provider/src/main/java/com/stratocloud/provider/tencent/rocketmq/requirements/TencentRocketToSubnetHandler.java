package com.stratocloud.provider.tencent.rocketmq.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.rocketmq.TencentRocketHandler;
import com.stratocloud.provider.tencent.subnet.TencentSubnetHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.trocket.v20230308.models.Endpoint;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TencentRocketToSubnetHandler implements EssentialRequirementHandler {

    private final TencentRocketHandler rocketHandler;

    private final TencentSubnetHandler subnetHandler;

    public TencentRocketToSubnetHandler(TencentRocketHandler rocketHandler,
                                        TencentSubnetHandler subnetHandler) {
        this.rocketHandler = rocketHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_ROCKETMQ_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云RocketMQ实例与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return rocketHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return subnetHandler;
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
        return "子网";
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
        TencentCloudProvider provider = (TencentCloudProvider) rocketHandler.getProvider();
        var detail = provider.buildClient(account).describeRocketInstanceDetail(source.externalId());

        if(detail.isEmpty())
            return List.of();

        Endpoint[] endpointList = detail.get().getEndpointList();

        if(endpointList == null)
            return List.of();

        Set<String> subnetIds = Arrays.stream(endpointList).map(Endpoint::getSubnetId).collect(Collectors.toSet());

        return subnetHandler.describeExternalResources(account, Map.of()).stream().filter(
                er -> subnetIds.contains(er.externalId())
        ).map(
                er -> new ExternalRequirement(
                        getRelationshipTypeId(),
                        er,
                        Map.of()
                )
        ).toList();
    }
}
