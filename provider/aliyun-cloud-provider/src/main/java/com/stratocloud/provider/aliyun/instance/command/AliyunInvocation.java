package com.stratocloud.provider.aliyun.instance.command;

import com.aliyun.ecs20140526.models.DescribeInvocationsResponseBody;

import java.util.Set;

public record AliyunInvocation(DescribeInvocationsResponseBody.DescribeInvocationsResponseBodyInvocationsInvocation detail) {

    public boolean isRunning(){
        return Set.of("Pending", "Scheduled", "Running").contains(detail.getInvocationStatus());
    }
}
