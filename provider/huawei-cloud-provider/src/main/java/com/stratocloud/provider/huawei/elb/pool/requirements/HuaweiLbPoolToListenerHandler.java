package com.stratocloud.provider.huawei.elb.pool.requirements;

import com.huaweicloud.sdk.elb.v3.model.Pool;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.listener.HuaweiListenerHandler;
import com.stratocloud.provider.huawei.elb.pool.HuaweiLbPoolHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.relationship.PrimaryCapabilityHandler;
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
public class HuaweiLbPoolToListenerHandler implements EssentialRequirementHandler, PrimaryCapabilityHandler {

    private final HuaweiLbPoolHandler poolHandler;

    private final HuaweiListenerHandler listenerHandler;

    public HuaweiLbPoolToListenerHandler(HuaweiLbPoolHandler poolHandler,
                                         HuaweiListenerHandler listenerHandler) {
        this.poolHandler = poolHandler;
        this.listenerHandler = listenerHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_LB_POOL_TO_LISTENER_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "监听器与后端服务器组";
    }

    @Override
    public ResourceHandler getSource() {
        return poolHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return listenerHandler;
    }

    @Override
    public String getCapabilityName() {
        return "后端服务器组";
    }

    @Override
    public String getRequirementName() {
        return "监听器";
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
        Optional<Pool> poolV2 = poolHandler.describeLbPool(account, source.externalId());

        if(poolV2.isEmpty())
            return List.of();

        var listeners = poolV2.get().getListeners();

        if(Utils.isEmpty(listeners))
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();
        for (var listenerRef : listeners) {
            var listener = listenerHandler.describeExternalResource(account, listenerRef.getId());
            listener.ifPresent(
                    l -> result.add(
                            new ExternalRequirement(getRelationshipTypeId(), l, Map.of())
                    )
            );
        }
        return result;
    }
}
