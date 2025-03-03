package com.stratocloud.provider.aliyun.common;

public record AliyunMetricDataPoint(Long timestamp,
                                    String userId,
                                    String instanceId,
                                    float Minimum,
                                    float Average,
                                    float Maximum,
                                    float Value) {
}
