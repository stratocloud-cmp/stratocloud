package com.stratocloud.provider.aliyun.redis.model;

import com.aliyun.r_kvstore20150101.models.DescribeInstancesResponseBody;

public record RedisInstance(DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesKVStoreInstance detail) {
}
