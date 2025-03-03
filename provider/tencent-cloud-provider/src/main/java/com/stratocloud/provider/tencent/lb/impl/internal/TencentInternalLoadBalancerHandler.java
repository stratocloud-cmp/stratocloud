package com.stratocloud.provider.tencent.lb.impl.internal;

import com.stratocloud.provider.ResourcePropertiesUtil;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.lb.TencentLoadBalancerHandler;
import com.tencentcloudapi.clb.v20180317.models.LoadBalancer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class TencentInternalLoadBalancerHandler extends TencentLoadBalancerHandler {

    public TencentInternalLoadBalancerHandler(TencentCloudProvider provider) {
        super(provider);
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_INTERNAL_LOAD_BALANCER";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云内网负载均衡";
    }

    @Override
    protected boolean filterLb(LoadBalancer loadBalancer) {
        return Objects.equals("INTERNAL", loadBalancer.getLoadBalancerType());
    }

    @Override
    public Map<String, Object> getPropertiesAtIndex(Map<String, Object> properties, int index) {
        return ResourcePropertiesUtil.getPropertiesAtIndex(properties, index, List.of("vips"));
    }
}
