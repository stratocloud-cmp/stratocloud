package com.stratocloud.resource.monitor;

import java.util.List;

public record Metric(String metricNamespace,
                     String metricName,
                     String metricLabel,
                     String metricUnit,
                     MetricType metricType,
                     MetricGroup metricGroup,
                     MetricValueType metricValueType,
                     boolean isPercentage,
                     List<Integer> supportedPeriodSeconds) {
}
