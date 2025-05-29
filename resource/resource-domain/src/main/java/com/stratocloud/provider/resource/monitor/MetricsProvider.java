package com.stratocloud.provider.resource.monitor;

import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.alert.AlertStatus;
import com.stratocloud.resource.alert.ExternalAlertHistory;
import com.stratocloud.resource.monitor.MetricData;
import com.stratocloud.resource.monitor.ResourceQuickStats;
import com.stratocloud.utils.Utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface MetricsProvider {

    List<SupportedMetric> getSupportedMetrics();

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
        List<SupportedMetric> quickStatsMetrics = getSupportedMetrics().stream().filter(
                SupportedMetric::isQuickStatsMetric
        ).filter(
                m -> m.resourceCategory().id().equals(resource.getCategory())
        ).toList();

        if(quickStatsMetrics.isEmpty())
            return Optional.empty();

        ResourceQuickStats.Builder builder = ResourceQuickStats.builder();

        for (SupportedMetric quickStatsMetric : quickStatsMetrics) {
            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.minusMinutes(10);
            MetricData metricData = describeMetricData(
                    resource,
                    quickStatsMetric,
                    from,
                    to,
                    quickStatsMetric.metric().supportedPeriodSeconds().get(0)
            );
            if(Utils.isNotEmpty(metricData.sequences())){
                double latestValue = metricData.sequences().get(0).latestValue();
                builder.addItem(
                        quickStatsMetric.metric().metricName(),
                        quickStatsMetric.metric().metricLabel(),
                        latestValue,
                        quickStatsMetric.metric().metricUnit()
                );
            }
        }

        return Optional.of(builder.build());
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
}
