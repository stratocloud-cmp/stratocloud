package com.stratocloud.provider.aliyun.eip.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.eip.AliyunBandwidthPackage;
import com.stratocloud.provider.aliyun.eip.AliyunBandwidthPackageHandler;
import com.stratocloud.provider.aliyun.zone.AliyunZoneHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunBandwidthPackageToZoneHandler implements EssentialRequirementHandler {

    private final AliyunBandwidthPackageHandler packageHandler;

    private final AliyunZoneHandler zoneHandler;

    public AliyunBandwidthPackageToZoneHandler(AliyunBandwidthPackageHandler packageHandler,
                                               AliyunZoneHandler zoneHandler) {
        this.packageHandler = packageHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_BWP_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云带宽包与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return packageHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "带宽包";
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
        Optional<AliyunBandwidthPackage> bwp = packageHandler.describePackage(account, source.externalId());

        if(bwp.isEmpty())
            return List.of();

        String zoneId = bwp.get().detail().getZone();

        if(Utils.isBlank(zoneId))
            return List.of();

        Optional<ExternalResource> zone = zoneHandler.describeExternalResource(account, zoneId);

        return zone.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
