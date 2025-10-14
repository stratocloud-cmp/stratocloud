package com.stratocloud.provider.aliyun.redis.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.redis.AliyunRedisHandler;
import com.stratocloud.provider.aliyun.redis.model.RedisInstance;
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
public class AliyunRedisToZoneHandler implements EssentialRequirementHandler {

    private final AliyunRedisHandler redisHandler;

    private final AliyunZoneHandler zoneHandler;

    public AliyunRedisToZoneHandler(AliyunRedisHandler redisHandler, AliyunZoneHandler zoneHandler) {
        this.redisHandler = redisHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_REDIS_TO_ZONE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "阿里云Redis实例与可用区";
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
        Optional<RedisInstance> redis = redisHandler.describeRedis(account, source.externalId());

        if(redis.isEmpty())
            return List.of();

        Optional<ExternalResource> zoneResource = zoneHandler.describeExternalResource(
                account,
                redis.get().detail().getZoneId()
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
