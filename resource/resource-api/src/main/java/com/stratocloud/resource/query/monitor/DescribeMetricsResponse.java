package com.stratocloud.resource.query.monitor;

import com.stratocloud.request.ApiResponse;
import com.stratocloud.resource.monitor.MetricGroupData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeMetricsResponse extends ApiResponse {
    private List<MetricGroupData> metricGroups;
}
