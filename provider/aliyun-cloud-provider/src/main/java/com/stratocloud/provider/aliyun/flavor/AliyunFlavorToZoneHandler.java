package com.stratocloud.provider.aliyun.flavor;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.zone.AliyunZoneHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunFlavorToZoneHandler implements EssentialRequirementHandler {

    private final AliyunFlavorHandler flavorHandler;

    private final AliyunZoneHandler zoneHandler;


    public AliyunFlavorToZoneHandler(AliyunFlavorHandler flavorHandler,
                                     AliyunZoneHandler zoneHandler) {
        this.flavorHandler = flavorHandler;
        this.zoneHandler = zoneHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_FLAVOR_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机规格与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return flavorHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "云主机规格";
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
        Optional<AliyunFlavor> flavor = flavorHandler.describeFlavor(account, source.externalId());

        if(flavor.isEmpty())
            return new ArrayList<>();

        Optional<ExternalResource> zone = zoneHandler.describeExternalResource(
                account, flavor.get().flavorId().zoneId()
        );

        return zone.map(externalResource -> List.of(new ExternalRequirement(getRelationshipTypeId(), externalResource, Map.of()))).orElseGet(ArrayList::new);

    }
}
