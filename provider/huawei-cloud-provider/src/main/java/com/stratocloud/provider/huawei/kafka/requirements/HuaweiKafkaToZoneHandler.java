package com.stratocloud.provider.huawei.kafka.requirements;

import com.huaweicloud.sdk.kafka.v2.model.AvailableZonesResp;
import com.huaweicloud.sdk.kafka.v2.model.ShowInstanceResp;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.kafka.HuaweiKafkaHandler;
import com.stratocloud.provider.huawei.zone.HuaweiZoneHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiKafkaToZoneHandler implements EssentialRequirementHandler {

    private final HuaweiKafkaHandler kafkaHandler;

    private final HuaweiZoneHandler zoneHandler;

    public HuaweiKafkaToZoneHandler(HuaweiKafkaHandler kafkaHandler, HuaweiZoneHandler zoneHandler) {
        this.kafkaHandler = kafkaHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_KAFKA_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云可用区与Kafka实例";
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
    public boolean visibleInTarget() {
        return false;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<ShowInstanceResp> instance = kafkaHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        List<String> azIds = instance.get().getAvailableZones();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) kafkaHandler.getProvider();
        List<AvailableZonesResp> zones = provider.buildClient(account).kafka().describeZones();

        List<String> azCodes = zones.stream().filter(
                z -> azIds.contains(z.getId())
        ).map(AvailableZonesResp::getCode).toList();

        if(Utils.isEmpty(azCodes))
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();

        for (String azCode : azCodes) {
            zoneHandler.describeExternalResource(account, azCode).ifPresent(
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
