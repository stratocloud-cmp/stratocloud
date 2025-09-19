package com.stratocloud.provider.tencent.kafka.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.kafka.TencentKafkaHandler;
import com.stratocloud.provider.tencent.kafka.TencentKafkaZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class TencentKafkaToZoneHandler implements EssentialRequirementHandler {

    private final TencentKafkaHandler kafkaHandler;

    private final TencentKafkaZoneHandler zoneHandler;

    public TencentKafkaToZoneHandler(TencentKafkaHandler kafkaHandler,
                                     TencentKafkaZoneHandler zoneHandler) {
        this.kafkaHandler = kafkaHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_KAFKA_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云Kafka实例与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return kafkaHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Kafka实例";
    }

    @Override
    public String getRequirementName() {
        return "可用区";
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
    public int compareRequirement(RelationshipHandler other) {
        return -1;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        var kafka = kafkaHandler.describeKafka(account, source.externalId());

        if(kafka.isEmpty())
            return List.of();

        Long[] zoneIds = Utils.isEmpty(kafka.get().getZoneIds()) ?
                new Long[] {kafka.get().getZoneId()} : kafka.get().getZoneIds();

        if(Utils.isEmpty(zoneIds))
            return List.of();

        return zoneHandler.describeExternalResources(account, Map.of()).stream().filter(
                r -> Arrays.stream(zoneIds).map(String::valueOf).toList().contains(r.externalId())
        ).map(
                r -> new ExternalRequirement(
                        getRelationshipTypeId(),
                        r,
                        Map.of()
                )
        ).toList();
    }
}
