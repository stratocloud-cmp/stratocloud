package com.stratocloud.provider.tencent.lb.listener.requirements;

import com.stratocloud.provider.tencent.lb.common.TencentListenerToLbHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentL7ListenerHandler;
import com.stratocloud.provider.tencent.lb.impl.open.TencentOpenLoadBalancerHandler;
import org.springframework.stereotype.Component;

@Component
public class TencentL7ListenerToOpenLbHandler extends TencentListenerToLbHandler {

    public static final String TYPE_ID = "TENCENT_L7_LISTENER_TO_OPEN_LB";

    public TencentL7ListenerToOpenLbHandler(TencentOpenLoadBalancerHandler loadBalancerHandler,
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
        return "公网负载均衡";
    }

    @Override
    public String getConnectActionName() {
        return "添加七层监听器";
    }

    @Override
    public String getDisconnectActionName() {
        return "移除七层监听器";
    }
}
