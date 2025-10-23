package com.stratocloud.provider.aliyun.rocket;

import com.aliyun.rocketmq20220801.models.ListInstancesResponseBody;

public record RocketInstance(ListInstancesResponseBody.ListInstancesResponseBodyDataList detail) {
}
