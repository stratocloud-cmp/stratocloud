package com.stratocloud.provider.tencent.kafka.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.kafka.TencentKafkaZoneHandler;
import com.stratocloud.provider.tencent.zone.TencentZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.cvm.v20170312.models.ZoneInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentKafkaZoneToZoneHandler implements EssentialRequirementHandler {

    private final TencentKafkaZoneHandler kafkaZoneHandler;

    private final TencentZoneHandler zoneHandler;

    public TencentKafkaZoneToZoneHandler(TencentKafkaZoneHandler kafkaZoneHandler, TencentZoneHandler zoneHandler) {
        this.kafkaZoneHandler = kafkaZoneHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_KAFKA_ZONE_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云Kafka可用区与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return kafkaZoneHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }

    @Override
    public String getCapabilityName() {
        return "Kafka可用区";
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        TencentCloudProvider provider = (TencentCloudProvider) zoneHandler.getProvider();

        Optional<ZoneInfo> zoneInfo = provider.buildClient(account).describeZones().stream().filter(
                z -> Objects.equals(z.getZoneId(), source.externalId())
        ).findAny();

        return zoneInfo.map(
                z -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                zoneHandler.toExternalResource(account, z),
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
