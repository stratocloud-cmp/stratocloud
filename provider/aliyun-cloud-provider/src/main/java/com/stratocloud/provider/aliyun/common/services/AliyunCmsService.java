package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.cms20190101.models.DescribeAlertLogListRequest;
import com.aliyun.cms20190101.models.DescribeAlertLogListResponseBody;
import com.aliyun.cms20190101.models.DescribeMetricLastRequest;
import com.aliyun.cms20190101.models.DescribeMetricListRequest;
import com.stratocloud.provider.aliyun.common.AliyunMetricDataPoint;

import java.util.List;

public interface AliyunCmsService {
    @SuppressWarnings("unused")
    List<AliyunMetricDataPoint> describeLatestMetrics(DescribeMetricLastRequest request);

    List<AliyunMetricDataPoint> describeMetricList(DescribeMetricListRequest request);

    List<DescribeAlertLogListResponseBody.DescribeAlertLogListResponseBodyAlertLogList> describeAlertHistories(DescribeAlertLogListRequest request);
}
