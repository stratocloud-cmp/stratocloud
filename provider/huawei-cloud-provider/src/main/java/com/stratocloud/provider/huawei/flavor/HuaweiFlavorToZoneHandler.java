package com.stratocloud.provider.huawei.flavor;

import com.huaweicloud.sdk.ecs.v2.model.Flavor;
import com.huaweicloud.sdk.ecs.v2.model.FlavorExtraSpec;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.zone.HuaweiZoneHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HuaweiFlavorToZoneHandler implements RelationshipHandler {

    private final HuaweiFlavorHandler flavorHandler;

    private final HuaweiZoneHandler zoneHandler;

    public HuaweiFlavorToZoneHandler(HuaweiFlavorHandler flavorHandler, HuaweiZoneHandler zoneHandler) {
        this.flavorHandler = flavorHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "OPENSTACK_FLAVOR_TO_ZONE_HANDLER";
    }

    @Override
    public String getRelationshipTypeName() {
        return "可用区与规格";
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
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<Flavor> flavor = flavorHandler.describeFlavor(account, source.externalId());

        if(flavor.isEmpty())
            return List.of();

        FlavorExtraSpec extraSpec = flavor.get().getOsExtraSpecs();

        if(extraSpec == null)
            return List.of();

        String condOperationAz = extraSpec.getCondOperationAz();
        Map<String, Boolean> zones = flavorHandler.getFlavorStatusInDifferentZones(condOperationAz);

        if(Utils.isEmpty(zones))
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();

        for (var entry : zones.entrySet()) {
            if(entry.getValue() != null && entry.getValue()) {
                Optional<ExternalResource> zone = zoneHandler.describeExternalResource(account, entry.getKey());

                if(zone.isEmpty())
                    continue;

                result.add(new ExternalRequirement(
                        getRelationshipTypeId(),
                        zone.get(),
                        Map.of()
                ));
            }
        }

        return result;
    }



    @Override
    public boolean disconnectOnLost() {
        return true;
    }
}
