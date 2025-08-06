package com.stratocloud.provider.resource.monitor;

import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.alert.AlertStatus;
import com.stratocloud.resource.alert.ExternalAlertHistory;
import com.stratocloud.resource.monitor.Metric;
import com.stratocloud.resource.monitor.MetricData;
import com.stratocloud.resource.monitor.MetricSequence;
import com.stratocloud.resource.monitor.ResourceQuickStats;
import com.stratocloud.utils.Utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MetricsProvider {

    List<SupportedMetric> getSupportedMetrics();

    default List<SupportedMetric> getSupportedMetrics(Resource resource){
        return getSupportedMetrics().stream().filter(
                m -> m.resourceCategory().id().equals(resource.getCategory())
        ).toList();
    }

    default int getMaxMetricsPullSize() {
        return 1400;
    }

    MetricData describeMetricData(Resource resource,
                                  SupportedMetric supportedMetric,
                                  LocalDateTime from,
                                  LocalDateTime to,
                                  int periodSeconds);

    List<ExternalAlertHistory> describeAlertHistories(Resource resource,
                                                      LocalDateTime happenedAfter);



    default Optional<ResourceQuickStats> describeQuickStats(Resource resource){
        List<SupportedMetric> quickStatsMetrics = getSupportedMetrics(resource).stream().filter(
                SupportedMetric::isQuickStatsMetric
        ).toList();

        if(quickStatsMetrics.isEmpty())
            return Optional.empty();

        ResourceQuickStats.Builder builder = ResourceQuickStats.builder();

        Map<Metric, String> shortMetricNames = getShortMetricNames();

        for (SupportedMetric quickStatsMetric : quickStatsMetrics) {
            int minPeriodSeconds = quickStatsMetric.metric().supportedPeriodSeconds().get(0);

            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.minusMinutes(Math.max(10, minPeriodSeconds * 2 / 60));
            MetricData metricData = describeMetricData(
                    resource,
                    quickStatsMetric,
                    from,
                    to,
                    minPeriodSeconds
            );
            List<MetricSequence> sequences = metricData.sequences();
            if(Utils.isNotEmpty(sequences)){
                double latestValue = getQuickStatsValue(quickStatsMetric, sequences);
                String shortName = shortMetricNames.get(quickStatsMetric.metric());

                builder.addItem(
                        Utils.isNotBlank(shortName) ? shortName : quickStatsMetric.metric().metricName(),
                        quickStatsMetric.metric().metricLabel(),
                        latestValue,
                        quickStatsMetric.metric().metricUnit(),
                        quickStatsMetric.metric().isPercentage()
                );
            }
        }

        return Optional.of(builder.build());
    }

    default double getQuickStatsValue(SupportedMetric quickStatsMetric, List<MetricSequence> sequences) {
        return sequences.get(0).latestValue();
    }


    default List<ExternalResourceEvent> describeAlertEvents(Resource resource,
                                                            LocalDateTime happenedAfter){
        List<ExternalAlertHistory> alertHistories = describeAlertHistories(resource, happenedAfter);

        List<ExternalResourceEvent> result = new ArrayList<>();

        for (ExternalAlertHistory alertHistory : alertHistories) {
            List<SupportedMetric> supportedMetrics = getSupportedMetrics().stream().filter(
                    m -> m.resourceCategory().id().equals(alertHistory.resourceCategory())
            ).filter(
                    m -> m.metric().metricName().equals(alertHistory.metricName())
            ).toList();
            if(supportedMetrics.isEmpty())
                continue;

            for (SupportedMetric supportedMetric : supportedMetrics) {
                if(supportedMetric.alertEventType().isPresent()) {
                    ExternalResourceEvent event = new ExternalResourceEvent(
                            alertHistory.id(),
                            supportedMetric.alertEventType().get(),
                            alertHistory.level(),
                            StratoEventSource.ALERT,
                            resource.getType(),
                            resource.getAccountId(),
                            resource.getExternalId(),
                            alertHistory.message(),
                            alertHistory.firstOccurredAt()
                    );
                    result.add(event);
                }

                if(supportedMetric.alertRecoveredEventType().isPresent() && alertHistory.alertStatus() == AlertStatus.OK){
                    ExternalResourceEvent event = new ExternalResourceEvent(
                            alertHistory.id(),
                            supportedMetric.alertRecoveredEventType().get(),
                            StratoEventLevel.REMIND,
                            StratoEventSource.ALERT,
                            resource.getType(),
                            resource.getAccountId(),
                            resource.getExternalId(),
                            "告警已恢复: "+alertHistory.message(),
                            alertHistory.firstOccurredAt()
                    );
                    result.add(event);
                }
            }
        }

        return result;
    }

    default Map<Metric, String> getShortMetricNames(){
        return Map.of();
    }
}
