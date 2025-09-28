package com.stratocloud.provider.aliyun.rds.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.rds.AliyunRdsHandler;
import com.stratocloud.provider.aliyun.rds.model.RdsInstance;
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
public class AliyunRdsToZoneHandler implements EssentialRequirementHandler {

    private final AliyunRdsHandler rdsHandler;

    private final AliyunZoneHandler zoneHandler;

    public AliyunRdsToZoneHandler(AliyunRdsHandler rdsHandler, AliyunZoneHandler zoneHandler) {
        this.rdsHandler = rdsHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_RDS_TO_ZONE_HANDLER";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云RDS实例与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return rdsHandler;
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
        return "RDS实例";
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
        Optional<RdsInstance> rdsInstance = rdsHandler.describeRds(account, source.externalId());

        if(rdsInstance.isEmpty())
            return List.of();

        Optional<ExternalResource> zoneResource = zoneHandler.describeExternalResource(
                account,
                rdsInstance.get().detail().getZoneId()
        );

        return zoneResource.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
