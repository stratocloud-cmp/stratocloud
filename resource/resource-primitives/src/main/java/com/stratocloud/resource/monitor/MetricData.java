package com.stratocloud.resource.monitor;

import java.util.List;

public record MetricData(Metric metric,
                         List<MetricSequence> sequences) {
}
