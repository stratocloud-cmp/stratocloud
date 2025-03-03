package com.stratocloud.provider.tencent.lb.listener.requirements;

import com.stratocloud.provider.tencent.lb.common.TencentListenerToLbHandler;
import com.stratocloud.provider.tencent.lb.listener.TencentL4ListenerHandler;
import com.stratocloud.provider.tencent.lb.impl.open.TencentOpenLoadBalancerHandler;
import org.springframework.stereotype.Component;

@Component
public class TencentL4ListenerToOpenLbHandler extends TencentListenerToLbHandler {

    public static final String TYPE_ID = "TENCENT_L4_LISTENER_TO_OPEN_LB_RELATIONSHIP";

    public TencentL4ListenerToOpenLbHandler(TencentOpenLoadBalancerHandler loadBalancerHandler,
                                            TencentL4ListenerHandler listenerHandler) {
        super(loadBalancerHandler, listenerHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getCapabilityName() {
        return "四层监听器";
    }

    @Override
    public String getRequirementName() {
        return "公网负载均衡";
    }

    @Override
    public String getConnectActionName() {
        return "添加四层监听器";
    }

    @Override
    public String getDisconnectActionName() {
        return "移除四层监听器";
    }
}
