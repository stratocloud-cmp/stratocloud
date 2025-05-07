package com.stratocloud.resource.monitor;

import java.util.ArrayList;
import java.util.List;

public class ResourceMonitorData {
    private final MetricInfo metricInfo;

    private final List<MetricPoint> metricPoints = new ArrayList<>();


    public ResourceMonitorData(MetricInfo metricInfo) {
        this.metricInfo = metricInfo;
    }

    public MetricInfo getMetricInfo() {
        return metricInfo;
    }

    public List<MetricPoint> getMetricPoints() {
        return metricPoints;
    }
}
