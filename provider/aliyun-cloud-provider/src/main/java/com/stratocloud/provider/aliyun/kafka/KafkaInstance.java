package com.stratocloud.provider.aliyun.kafka;

import com.aliyun.alikafka20190916.models.GetInstanceListResponseBody;

public record KafkaInstance(GetInstanceListResponseBody.GetInstanceListResponseBodyInstanceListInstanceVO detail) {
}
