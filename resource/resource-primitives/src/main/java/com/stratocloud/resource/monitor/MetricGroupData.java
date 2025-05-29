package com.stratocloud.resource.monitor;

import java.util.List;

public record MetricGroupData(MetricGroup group,
                              List<MetricData> metrics) {
}
