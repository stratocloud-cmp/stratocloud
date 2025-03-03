package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.cms20190101.models.DescribeMetricLastRequest;
import com.stratocloud.provider.aliyun.common.AliyunMetricDataPoint;

import java.util.List;

public interface AliyunCmsService {
    List<AliyunMetricDataPoint> describeLatestMetrics(DescribeMetricLastRequest request);
}
