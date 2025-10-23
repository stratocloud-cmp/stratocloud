package com.stratocloud.provider.aliyun.rocket.requirements;

import com.aliyun.rocketmq20220801.models.GetInstanceResponseBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.rocket.AliyunRocketHandler;
import com.stratocloud.provider.aliyun.rocket.RocketInstance;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnetHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunRocketToSubnetHandler implements RelationshipHandler {

    private final AliyunRocketHandler rocketHandler;

    private final AliyunSubnetHandler subnetHandler;

    public AliyunRocketToSubnetHandler(AliyunRocketHandler rocketHandler, AliyunSubnetHandler subnetHandler) {
        this.rocketHandler = rocketHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_ROCKETMQ_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云RocketMQ实例与子网";
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
        Optional<RocketInstance> rocketInstance = rocketHandler.describeInstance(account, source.externalId());

        if(rocketInstance.isEmpty())
            return List.of();

        AliyunCloudProvider provider = (AliyunCloudProvider) rocketHandler.getProvider();
        AliyunClient client = provider.buildClient(account);
        var detail = client.rocket().describeInstanceDetail(source.externalId());

        GetInstanceResponseBody.GetInstanceResponseBodyDataNetworkInfo networkInfo = detail.getNetworkInfo();
        if(networkInfo==null || networkInfo.getVpcInfo()==null || Utils.isEmpty(networkInfo.getVpcInfo().getVSwitches()))
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();
        for (var vSwitch : networkInfo.getVpcInfo().getVSwitches()) {
            String vSwitchId = vSwitch.getVSwitchId();
            subnetHandler.describeExternalResource(account, vSwitchId).ifPresent(
                    er -> result.add(
                            new ExternalRequirement(
                                    getRelationshipTypeId(),
                                    er,
                                    Map.of()
                            )
                    )
            );
        }
        return result;
    }
}
