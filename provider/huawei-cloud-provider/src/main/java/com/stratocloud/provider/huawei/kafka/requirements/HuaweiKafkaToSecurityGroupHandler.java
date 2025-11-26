package com.stratocloud.provider.huawei.kafka.requirements;

import com.huaweicloud.sdk.kafka.v2.model.ShowInstanceResp;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.kafka.HuaweiKafkaHandler;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiKafkaToSecurityGroupHandler implements EssentialRequirementHandler {

    private final HuaweiKafkaHandler kafkaHandler;

    private final HuaweiSecurityGroupHandler securityGroupHandler;

    public HuaweiKafkaToSecurityGroupHandler(HuaweiKafkaHandler kafkaHandler, HuaweiSecurityGroupHandler securityGroupHandler) {
        this.kafkaHandler = kafkaHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_KAFKA_TO_SECURITY_GROUP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云Kafka实例与安全组";
    }

    @Override
    public ResourceHandler getSource() {
        return kafkaHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return securityGroupHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Kafka实例";
    }

    @Override
    public String getRequirementName() {
        return "安全组";
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

        String securityGroupId = instance.get().getSecurityGroupId();

        Optional<ExternalResource> securityGroup = securityGroupHandler.describeExternalResource(account, securityGroupId);

        return securityGroup.map(s -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        s,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
