package com.stratocloud.provider.aliyun.common.services;

import com.stratocloud.provider.aliyun.common.AliyunEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface AliyunTrailService {
    List<AliyunEvent> describeEvents(List<String> eventNames,
                                     String resourceType,
                                     String resourceId,
                                     LocalDateTime startTime);
}
