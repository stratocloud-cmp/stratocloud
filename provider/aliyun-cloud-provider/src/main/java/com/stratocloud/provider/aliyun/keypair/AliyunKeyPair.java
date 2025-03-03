package com.stratocloud.provider.aliyun.keypair;

import com.aliyun.ecs20140526.models.DescribeKeyPairsResponseBody;

public record AliyunKeyPair(DescribeKeyPairsResponseBody.DescribeKeyPairsResponseBodyKeyPairsKeyPair detail) {
}
