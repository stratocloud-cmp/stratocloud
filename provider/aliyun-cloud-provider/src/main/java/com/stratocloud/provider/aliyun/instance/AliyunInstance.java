package com.stratocloud.provider.aliyun.instance;

import com.aliyun.ecs20140526.models.DescribeInstancesResponseBody;

public record AliyunInstance(
        DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstance detail
) {
    public boolean isWindows() {
        return "windows".equalsIgnoreCase(detail.getOSType());
    }
}
