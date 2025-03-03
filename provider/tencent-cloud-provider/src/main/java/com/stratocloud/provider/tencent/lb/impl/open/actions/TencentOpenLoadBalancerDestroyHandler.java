package com.stratocloud.provider.tencent.lb.impl.open.actions;

import com.stratocloud.provider.tencent.lb.common.TencentLoadBalancerDestroyHandler;
import com.stratocloud.provider.tencent.lb.impl.open.TencentOpenLoadBalancerHandler;
import org.springframework.stereotype.Component;

@Component
public class TencentOpenLoadBalancerDestroyHandler extends TencentLoadBalancerDestroyHandler {
    public TencentOpenLoadBalancerDestroyHandler(TencentOpenLoadBalancerHandler loadBalancerHandler) {
        super(loadBalancerHandler);
    }
}
