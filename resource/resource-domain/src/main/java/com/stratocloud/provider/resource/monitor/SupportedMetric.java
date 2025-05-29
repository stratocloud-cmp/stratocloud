package com.stratocloud.provider.resource.monitor;

import com.stratocloud.event.StratoEventType;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCategory;
import com.stratocloud.resource.monitor.Metric;
import com.stratocloud.resource.monitor.MetricObject;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record SupportedMetric(Metric metric,
                              String displayDimensionName,
                              Function<Resource, List<MetricObject>> objectsGetter,
                              Optional<StratoEventType> alertEventType,
                              Optional<StratoEventType> alertRecoveredEventType,
                              boolean isQuickStatsMetric,
                              ResourceCategory resourceCategory) {
}
