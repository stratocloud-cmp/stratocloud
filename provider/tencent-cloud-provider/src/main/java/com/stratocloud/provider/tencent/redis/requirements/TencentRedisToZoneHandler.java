package com.stratocloud.provider.tencent.redis.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
import com.stratocloud.provider.tencent.zone.TencentZoneHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.redis.v20180412.models.ZoneCapacityConf;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentRedisToZoneHandler implements EssentialRequirementHandler {

    private final TencentRedisHandler redisHandler;

    private final TencentZoneHandler zoneHandler;

    public TencentRedisToZoneHandler(TencentRedisHandler redisHandler,
                                     TencentZoneHandler zoneHandler) {
        this.redisHandler = redisHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_REDIS_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云Redis实例与主可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return redisHandler;
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
        return "Redis实例";
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
        var redis = redisHandler.describeRedis(account, source.externalId());

        if(redis.isEmpty())
            return List.of();

        Long zoneId = redis.get().getZoneId();

        if(zoneId == null)
            return List.of();

        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        List<ZoneCapacityConf> zoneConfigs = provider.buildClient(account).describeRedisZoneConfigs();

        Optional<ZoneCapacityConf> zoneConfig = zoneConfigs.stream().filter(
                c -> Objects.equals(zoneId, c.getOldZoneId())
        ).findAny();

        if(zoneConfig.isEmpty())
            return List.of();

        Optional<ExternalResource> zoneResource = zoneHandler.describeExternalResource(
                account, zoneConfig.get().getZoneId()
        );

        return zoneResource.map(
                er -> List.of(
                        new ExternalRequirement(
                                getRelationshipTypeId(),
                                er,
                                Map.of()
                        )
                )
        ).orElseGet(List::of);
    }
}
