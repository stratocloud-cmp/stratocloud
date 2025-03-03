package com.stratocloud.provider.tencent.lb.impl.internal.actions;

import com.stratocloud.provider.tencent.lb.common.TencentLoadBalancerDestroyHandler;
import com.stratocloud.provider.tencent.lb.impl.internal.TencentInternalLoadBalancerHandler;
import org.springframework.stereotype.Component;

@Component
public class TencentInternalLoadBalancerDestroyHandler extends TencentLoadBalancerDestroyHandler {
    public TencentInternalLoadBalancerDestroyHandler(TencentInternalLoadBalancerHandler loadBalancerHandler) {
        super(loadBalancerHandler);
    }
}
