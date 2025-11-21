package com.stratocloud.provider.huawei.dcs.requirements;

import com.huaweicloud.sdk.dcs.v2.model.InstanceListInfo;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.dcs.HuaweiDcsHandler;
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
public class HuaweiDcsToZoneHandler implements EssentialRequirementHandler {

    private final HuaweiDcsHandler dcsHandler;

    private final HuaweiZoneHandler zoneHandler;

    public HuaweiDcsToZoneHandler(HuaweiDcsHandler dcsHandler, HuaweiZoneHandler zoneHandler) {
        this.dcsHandler = dcsHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_DCS_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云可用区与Redis实例";
    }

    @Override
    public ResourceHandler getSource() {
        return dcsHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "Redis实例";
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
        Optional<InstanceListInfo> instance = dcsHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        List<String> azCodes = instance.get().getAzCodes();

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
