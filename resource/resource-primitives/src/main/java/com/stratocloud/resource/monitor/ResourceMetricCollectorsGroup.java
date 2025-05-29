package com.stratocloud.resource.monitor;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResourceMetricCollectorsGroup {
    private MetricGroup type;
    private List<ResourceMetricCollector> collectors;
}
