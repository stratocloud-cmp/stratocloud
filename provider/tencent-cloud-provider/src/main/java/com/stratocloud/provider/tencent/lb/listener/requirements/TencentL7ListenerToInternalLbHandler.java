package com.stratocloud.provider.tencent.lb.listener.requirements;

import com.stratocloud.provider.tencent.lb.common.TencentListenerToLbHandler;
import com.stratocloud.provider.tencent.lb.impl.internal.TencentInternalLoadBalancerHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentL7ListenerHandler;
import org.springframework.stereotype.Component;

@Component
public class TencentL7ListenerToInternalLbHandler extends TencentListenerToLbHandler {

    public static final String TYPE_ID = "TENCENT_L7_LISTENER_TO_INTERNAL_LB";

    public TencentL7ListenerToInternalLbHandler(TencentInternalLoadBalancerHandler loadBalancerHandler,
                                                TencentL7ListenerHandler listenerHandler) {
        super(loadBalancerHandler, listenerHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getCapabilityName() {
        return "七层监听器";
    }

    @Override
    public String getRequirementName() {
        return "内网负载均衡";
    }

    @Override
    public String getConnectActionName() {
        return "添加七层监听器";
    }

    @Override
    public String getDisconnectActionName() {
        return "移除监听器";
    }
}
