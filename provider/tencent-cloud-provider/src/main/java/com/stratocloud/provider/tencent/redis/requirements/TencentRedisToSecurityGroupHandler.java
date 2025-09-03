package com.stratocloud.provider.tencent.redis.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
import com.stratocloud.provider.tencent.securitygroup.TencentSecurityGroupHandler;
import com.stratocloud.resource.*;
import com.tencentcloudapi.redis.v20180412.models.SecurityGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TencentRedisToSecurityGroupHandler implements RelationshipHandler {

    private final TencentRedisHandler redisHandler;

    private final TencentSecurityGroupHandler securityGroupHandler;

    public TencentRedisToSecurityGroupHandler(TencentRedisHandler redisHandler,
                                              TencentSecurityGroupHandler securityGroupHandler) {
        this.redisHandler = redisHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_REDIS_TO_SECURITY_GROUP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "腾讯云Redis实例与安全组";
    }

    @Override
    public ResourceHandler getSource() {
        return redisHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return securityGroupHandler;
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
        return "安全组";
    }

    @Override
    public String getConnectActionName() {
        return "绑定";
    }

    @Override
    public String getDisconnectActionName() {
        return "解绑";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource redis = relationship.getSource();
        Resource securityGroup = relationship.getTarget();

        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(redis.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        client.associateRedisSecurityGroup(redis.getExternalId(), securityGroup.getExternalId());
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource redis = relationship.getSource();
        Resource securityGroup = relationship.getTarget();

        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(redis.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        client.disassociateRedisSecurityGroup(redis.getExternalId(), securityGroup.getExternalId());
    }

    @Override
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);
        List<SecurityGroup> securityGroups = client.describeRedisSecurityGroups(source.externalId());

        List<ExternalRequirement> result = new ArrayList<>();

        for (SecurityGroup securityGroup : securityGroups) {
            var sgResource = securityGroupHandler.describeExternalResource(account, securityGroup.getSecurityGroupId());

            sgResource.ifPresent(
                    r -> result.add(
                            new ExternalRequirement(
                                    getRelationshipTypeId(),
                                    r,
                                    Map.of()
                            )
                    )
            );
        }

        return result;
    }
}
