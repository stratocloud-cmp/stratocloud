package com.stratocloud.provider.aliyun.common;

public record AliyunMetricDataPoint(Long timestamp,
                                    String userId,
                                    String instanceId,
                                    String device,
                                    String id_serial,
                                    String diskname,
                                    String hostname,
                                    float Minimum,
                                    float Average,
                                    float Maximum,
                                    float Value) {
}
