package com.stratocloud.provider.aliyun.kafka.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.kafka.AliyunKafkaHandler;
import com.stratocloud.provider.aliyun.kafka.KafkaInstance;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnetHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunKafkaToSubnetHandler implements EssentialRequirementHandler {

    private final AliyunKafkaHandler kafkaHandler;

    private final AliyunSubnetHandler subnetHandler;

    public AliyunKafkaToSubnetHandler(AliyunKafkaHandler kafkaHandler, AliyunSubnetHandler subnetHandler) {
        this.kafkaHandler = kafkaHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_KAFKA_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云Kafka实例与子网";
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
    public boolean visibleInTarget() {
        return false;
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<KafkaInstance> kafkaInstance = kafkaHandler.describeInstance(account, source.externalId());

        if(kafkaInstance.isEmpty())
            return List.of();

        String vSwitchId = kafkaInstance.get().detail().getVSwitchId();

        return subnetHandler.describeExternalResource(account, vSwitchId).map(
                er -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                er,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
