package com.stratocloud.provider.aliyun.lb.classic;

import com.aliyun.slb20140515.models.DescribeLoadBalancersResponseBody;

public record AliyunClb(
        DescribeLoadBalancersResponseBody.DescribeLoadBalancersResponseBodyLoadBalancersLoadBalancer detail
) {
}
