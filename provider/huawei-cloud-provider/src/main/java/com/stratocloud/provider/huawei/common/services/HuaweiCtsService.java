package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.cts.v3.model.Traces;

import java.time.LocalDateTime;
import java.util.List;

public interface HuaweiCtsService {

    List<Traces> describeEvents(List<String> traceNames,
                                String resourceType,
                                String resourceId,
                                LocalDateTime startTime);
}
