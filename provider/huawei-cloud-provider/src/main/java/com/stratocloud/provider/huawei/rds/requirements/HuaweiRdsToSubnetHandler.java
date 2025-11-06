package com.stratocloud.provider.huawei.rds.requirements;

import com.huaweicloud.sdk.rds.v3.model.InstanceResponse;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
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
public class HuaweiRdsToSubnetHandler implements EssentialRequirementHandler {

    private final HuaweiRdsHandler rdsHandler;

    private final HuaweiSubnetHandler subnetHandler;

    public HuaweiRdsToSubnetHandler(HuaweiRdsHandler rdsHandler, HuaweiSubnetHandler subnetHandler) {
        this.rdsHandler = rdsHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_RDS_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云RDS实例与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return rdsHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return subnetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "RDS实例";
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
        Optional<InstanceResponse> instance = rdsHandler.describeInstance(account, source.externalId());

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
