package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.ces.v1.model.Datapoint;
import com.huaweicloud.sdk.ces.v1.model.ShowMetricDataRequest;

import java.util.List;

public interface HuaweiCesService {
    List<Datapoint> describeMetricData(ShowMetricDataRequest request);
}
