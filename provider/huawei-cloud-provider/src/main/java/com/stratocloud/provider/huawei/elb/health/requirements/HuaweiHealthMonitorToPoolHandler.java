package com.stratocloud.provider.huawei.elb.health.requirements;

import com.huaweicloud.sdk.elb.v3.model.HealthMonitor;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.health.HuaweiHealthMonitorHandler;
import com.stratocloud.provider.huawei.elb.pool.HuaweiLbPoolHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.relationship.PrimaryCapabilityHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiHealthMonitorToPoolHandler
        implements EssentialRequirementHandler, PrimaryCapabilityHandler {

    private final HuaweiHealthMonitorHandler healthMonitorHandler;

    private final HuaweiLbPoolHandler poolHandler;

    public HuaweiHealthMonitorToPoolHandler(HuaweiHealthMonitorHandler healthMonitorHandler,
                                            HuaweiLbPoolHandler poolHandler) {
        this.healthMonitorHandler = healthMonitorHandler;
        this.poolHandler = poolHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_HEALTH_MONITOR_TO_POOL_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "后端服务器组与健康检查";
    }

    @Override
    public ResourceHandler getSource() {
        return healthMonitorHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return poolHandler;
    }

    @Override
    public String getCapabilityName() {
        return "健康检查";
    }

    @Override
    public String getRequirementName() {
        return "后端服务器组";
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
        Optional<HealthMonitor> monitor = healthMonitorHandler.describeMonitor(account, source.externalId());

        if(monitor.isEmpty())
            return List.of();

        var pools = monitor.get().getPools();

        if(Utils.isEmpty(pools))
            return List.of();

        Optional<ExternalResource> pool = poolHandler.describeExternalResource(account, pools.get(0).getId());

        return pool.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
