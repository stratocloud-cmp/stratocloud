package com.stratocloud.provider.huawei.dcs.requirements;

import com.huaweicloud.sdk.dcs.v2.model.InstanceListInfo;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.dcs.HuaweiDcsHandler;
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
public class HuaweiDcsToSubnetHandler implements EssentialRequirementHandler {

    private final HuaweiDcsHandler dcsHandler;

    private final HuaweiSubnetHandler subnetHandler;

    public HuaweiDcsToSubnetHandler(HuaweiDcsHandler dcsHandler, HuaweiSubnetHandler subnetHandler) {
        this.dcsHandler = dcsHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_DCS_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云Redis实例与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return dcsHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return subnetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Redis实例";
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
        Optional<InstanceListInfo> instance = dcsHandler.describeInstance(account, source.externalId());

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
