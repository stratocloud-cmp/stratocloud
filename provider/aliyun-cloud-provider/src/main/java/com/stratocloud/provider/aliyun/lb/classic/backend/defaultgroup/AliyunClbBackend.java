package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup;

import com.aliyun.slb20140515.models.DescribeHealthStatusResponseBody;
import com.aliyun.slb20140515.models.DescribeLoadBalancerAttributeResponseBody;

public record AliyunClbBackend(
        AliyunClbBackendId id,
        DescribeLoadBalancerAttributeResponseBody.DescribeLoadBalancerAttributeResponseBodyBackendServersBackendServer detail,
        DescribeHealthStatusResponseBody.DescribeHealthStatusResponseBodyBackendServersBackendServer health
) {

    public String name(){
        return health.protocol+":"+health.serverIp + ":" + health.port;
    }

}
