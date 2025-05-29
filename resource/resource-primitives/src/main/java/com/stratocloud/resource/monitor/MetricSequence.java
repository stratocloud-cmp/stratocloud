package com.stratocloud.resource.monitor;

import com.stratocloud.utils.Utils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record MetricSequence(String sequenceName,
                             String sequenceDescription,
                             double maxValue,
                             double minValue,
                             double avgValue,
                             double latestValue,
                             List<MetricDataPoint> points) {

    public static Optional<MetricSequence> of(String sequenceName,
                                              String sequenceDescription,
                                              List<MetricDataPoint> points){
        if(Utils.isEmpty(points))
            return Optional.empty();

        double max = points.stream().mapToDouble(
                MetricDataPoint::value
        ).max().orElseThrow();

        double min = points.stream().mapToDouble(
                MetricDataPoint::value
        ).min().orElseThrow();

        double avg = points.stream().mapToDouble(
                MetricDataPoint::value
        ).average().orElseThrow();

        double latest = points.stream().max(
                Comparator.comparing(MetricDataPoint::time)
        ).map(MetricDataPoint::value).orElseThrow();

        return Optional.of(new MetricSequence(
                sequenceName,
                sequenceDescription,
                max,
                min,
                avg,
                latest,
                points
        ));
    }

}
