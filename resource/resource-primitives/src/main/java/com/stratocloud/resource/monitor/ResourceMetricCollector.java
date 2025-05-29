package com.stratocloud.resource.monitor;

import lombok.*;

import java.util.List;
import java.util.concurrent.Callable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResourceMetricCollector {
    private String metricName;
    private String metricLabel;
    private String metricUnit;
    private MetricType metricType;
    private Callable<List<MetricSequence>> collectTask;
}
