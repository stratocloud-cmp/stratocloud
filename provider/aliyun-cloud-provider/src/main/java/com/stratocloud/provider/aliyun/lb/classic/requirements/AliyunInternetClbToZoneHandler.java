package com.stratocloud.provider.aliyun.lb.classic.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.lb.classic.AliyunClb;
import com.stratocloud.provider.aliyun.lb.classic.AliyunInternetClbHandler;
import com.stratocloud.provider.aliyun.zone.AliyunZoneHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunInternetClbToZoneHandler implements EssentialRequirementHandler {

    private final AliyunInternetClbHandler clbHandler;

    private final AliyunZoneHandler zoneHandler;

    public AliyunInternetClbToZoneHandler(AliyunInternetClbHandler clbHandler,
                                          AliyunZoneHandler zoneHandler) {
        this.clbHandler = clbHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_INTERNET_CLB_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云公网CLB与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return clbHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "公网CLB";
    }

    @Override
    public String getRequirementName() {
        return "主可用区";
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
        Optional<AliyunClb> clb = clbHandler.describeClb(account, source.externalId());

        if(clb.isEmpty())
            return List.of();

        String masterZoneId = clb.get().detail().getMasterZoneId();

        Optional<ExternalResource> zone = zoneHandler.describeExternalResource(account, masterZoneId);

        if(zone.isEmpty())
            return List.of();

        return List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        zone.get(),
                        Map.of()
                )
        );
    }
}
