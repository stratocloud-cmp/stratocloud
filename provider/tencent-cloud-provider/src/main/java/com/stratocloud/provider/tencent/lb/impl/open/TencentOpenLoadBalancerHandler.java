package com.stratocloud.provider.tencent.lb.impl.open;

import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.TencentLoadBalancerHandler;
import com.tencentcloudapi.clb.v20180317.models.LoadBalancer;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TencentOpenLoadBalancerHandler extends TencentLoadBalancerHandler {

    public TencentOpenLoadBalancerHandler(TencentCloudProvider provider) {
        super(provider);
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_OPEN_LOAD_BALANCER";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云公网负载均衡";
    }


    @Override
    protected boolean filterLb(LoadBalancer loadBalancer) {
        return Objects.equals("OPEN", loadBalancer.getLoadBalancerType());
    }
}
