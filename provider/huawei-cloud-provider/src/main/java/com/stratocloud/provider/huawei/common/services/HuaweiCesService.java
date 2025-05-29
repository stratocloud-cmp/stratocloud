package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.ces.v1.model.Datapoint;
import com.huaweicloud.sdk.ces.v1.model.ShowMetricDataRequest;
import com.huaweicloud.sdk.ces.v2.model.AlarmHistoryItemV2;
import com.huaweicloud.sdk.ces.v2.model.ListAlarmHistoriesRequest;

import java.util.List;

public interface HuaweiCesService {
    List<Datapoint> describeMetricData(ShowMetricDataRequest request);

    List<AlarmHistoryItemV2> describeAlarmHistories(ListAlarmHistoriesRequest request);
}
