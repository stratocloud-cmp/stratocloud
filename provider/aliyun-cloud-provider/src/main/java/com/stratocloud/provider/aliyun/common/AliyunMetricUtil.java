package com.stratocloud.provider.aliyun.common;

import com.aliyun.cms20190101.models.DescribeMetricLastRequest;
import com.stratocloud.provider.aliyun.common.services.AliyunCmsService;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AliyunMetricUtil {
    public static Optional<Float> getLatestMetric(AliyunCmsService cmsService,
                                                  String namespace,
                                                  String metricName,
                                                  String instanceId,
                                                  Integer period,
                                                  Function<AliyunMetricDataPoint, Float> valueGetter){
        DescribeMetricLastRequest request = new DescribeMetricLastRequest();
        request.setNamespace(namespace);
        request.setMetricName(metricName);
        request.setDimensions(
                JSON.toJsonString(
                        List.of(
                                Map.of("instanceId", instanceId)
                        )
                )
        );
        request.setLength("10");
        request.setPeriod(period.toString());

        List<AliyunMetricDataPoint> dataPoints = cmsService.describeLatestMetrics(request);

        if(Utils.isEmpty(dataPoints))
            return Optional.empty();

        return Optional.of(valueGetter.apply(dataPoints.get(dataPoints.size()-1)));
    }
}
