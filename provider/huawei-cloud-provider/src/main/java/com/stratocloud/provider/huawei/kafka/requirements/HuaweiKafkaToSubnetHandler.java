package com.stratocloud.provider.huawei.kafka.requirements;

import com.huaweicloud.sdk.kafka.v2.model.ShowInstanceResp;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.kafka.HuaweiKafkaHandler;
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
public class HuaweiKafkaToSubnetHandler implements EssentialRequirementHandler {

    private final HuaweiKafkaHandler kafkaHandler;

    private final HuaweiSubnetHandler subnetHandler;

    public HuaweiKafkaToSubnetHandler(HuaweiKafkaHandler kafkaHandler, HuaweiSubnetHandler subnetHandler) {
        this.kafkaHandler = kafkaHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_KAFKA_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云Kafka实例与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return kafkaHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return subnetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Kafka实例";
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
    public boolean visibleInTarget() {
        return false;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<ShowInstanceResp> instance = kafkaHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        String subnetId = instance.get().getSubnetId();

        Optional<ExternalResource> subnet = subnetHandler.describeExternalResource(account, subnetId);

        return subnet.map(s -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        s,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
