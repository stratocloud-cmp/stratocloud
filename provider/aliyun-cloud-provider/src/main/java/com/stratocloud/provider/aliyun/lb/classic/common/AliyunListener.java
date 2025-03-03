package com.stratocloud.provider.aliyun.lb.classic.common;

import com.aliyun.slb20140515.models.DescribeLoadBalancerListenersResponseBody;

public record AliyunListener(
        DescribeLoadBalancerListenersResponseBody.DescribeLoadBalancerListenersResponseBodyListeners detail
) {
    public AliyunListenerId listenerId(){
        return new AliyunListenerId(
                detail.getLoadBalancerId(),
                detail.getListenerProtocol(),
                detail.getListenerPort().toString()
        );
    }
}
