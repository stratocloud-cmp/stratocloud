package com.stratocloud.provider.huawei.metrics;

import com.huaweicloud.sdk.ces.v1.model.Datapoint;
import com.huaweicloud.sdk.ces.v1.model.ShowMetricDataRequest;
import com.huaweicloud.sdk.ces.v2.model.AlarmHistoryItemV2;
import com.huaweicloud.sdk.ces.v2.model.ListAlarmHistoriesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.provider.ResourceEventTypes;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.provider.resource.monitor.SupportedMetric;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.alert.AlertStatus;
import com.stratocloud.resource.alert.ExternalAlertHistory;
import com.stratocloud.resource.monitor.*;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;

@Component
public class HuaweiMetricsProvider implements MetricsProvider {
    @Override
    public List<SupportedMetric> getSupportedMetrics() {
        return List.of(
                CPU_UTIL, CPU_USAGE_IDLE, CPU_USAGE_SYSTEM, CPU_USAGE_USER, CPU_USAGE_OTHER, CPU_USAGE_IOWAIT,
                CPU_LOAD_AVERAGE_1, CPU_LOAD_AVERAGE_5, CPU_LOAD_AVERAGE_15,
                MEM_UTIL,
                DISK_READ_RATE, DISK_READ_IOPS, DISK_WRITE_RATE, DISK_WRITE_IOPS,
                NETWORK_BANDWIDTH_OUT, NETWORK_BANDWIDTH_IN, NETWORK_PPS_OUT, NETWORK_PPS_IN, NETWORK_NEW_CONNECTIONS
        );
    }

    private static List<MetricObject> getInstanceMetricObjects(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();
        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("instance_id", resource.getExternalId())
                        )
                )
        );
    }

    @Override
    public MetricData describeMetricData(Resource resource,
                                         SupportedMetric supportedMetric,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         int periodSeconds) {
        List<MetricSequence> sequences = new ArrayList<>();

        Metric metric = supportedMetric.metric();
        List<MetricObject> metricObjects = supportedMetric.objectsGetter().apply(resource);

        if(Utils.isEmpty(metricObjects))
            return new MetricData(metric, sequences);

        ShowMetricDataRequest request = new ShowMetricDataRequest();
        request.setNamespace(metric.metricNamespace());
        request.setMetricName(metric.metricName());

        request.setFilter(getFilter(metric));

        MetricObject metricObject = metricObjects.get(0);

        if(Utils.isEmpty(metricObject.dimensions()))
            return new MetricData(metric, sequences);

        for (int i = 0; i < metricObject.dimensions().size(); i++) {
            Consumer<String> dimensionSetter = switch (i) {
                case 0 -> request::setDim0;
                case 1 -> request::setDim1;
                case 2 -> request::setDim2;
                case 3 -> request::setDim3;
                default -> s -> {};
            };
            MetricDimension metricDimension = metricObject.dimensions().get(i);
            dimensionSetter.accept(
                    "%s,%s".formatted(
                            metricDimension.name(),
                            metricDimension.value()
                    )
            );
        }


        if(periodSeconds < 300L)
            request.setPeriod(1);
        else
            request.setPeriod(periodSeconds);

        request.setFrom(from.atZone(TimeUtil.BEIJING_ZONE_ID).toInstant().toEpochMilli());
        request.setTo(to.atZone(TimeUtil.BEIJING_ZONE_ID).toInstant().toEpochMilli());

        List<Datapoint> dataPoints = getClient(resource).ces().describeMetricData(request);

        List<MetricDataPoint> metricDataPoints = new ArrayList<>();
        if(Utils.isNotEmpty(dataPoints)) {
            for (Datapoint dataPoint : dataPoints) {
                metricDataPoints.add(
                        new MetricDataPoint(
                                getMetricValue(dataPoint, metric.metricValueType()),
                                ZonedDateTime.ofInstant(
                                        Instant.ofEpochMilli(dataPoint.getTimestamp()),
                                        TimeUtil.BEIJING_ZONE_ID
                                ).toLocalDateTime()
                        )
                );
            }
        }

        MetricSequence.of(
                metricObject.getDimension(
                        supportedMetric.displayDimensionName()
                ).map(MetricDimension::value).orElse(resource.getExternalId()),
                null,
                metricDataPoints
        ).ifPresent(sequences::add);

        return new MetricData(metric, sequences);
    }



    private double getMetricValue(Datapoint dataPoint, MetricValueType metricValueType) {
        return switch (metricValueType) {
            case AVG, VALUE -> dataPoint.getAverage();
            case MAX -> dataPoint.getMax();
            case MIN -> dataPoint.getMin();
        };
    }

    private HuaweiCloudClient getClient(Resource resource) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) resource.getResourceHandler().getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(resource.getAccountId());
        return provider.buildClient(account);
    }

    private ShowMetricDataRequest.FilterEnum getFilter(Metric metric) {
        return switch (metric.metricValueType()){
            case AVG, VALUE -> ShowMetricDataRequest.FilterEnum.AVERAGE;
            case MIN -> ShowMetricDataRequest.FilterEnum.MIN;
            case MAX -> ShowMetricDataRequest.FilterEnum.MAX;
        };
    }

    @Override
    public List<ExternalAlertHistory> describeAlertHistories(Resource resource, LocalDateTime happenedAfter) {
        HuaweiCloudClient client = getClient(resource);

        ListAlarmHistoriesRequest request = new ListAlarmHistoriesRequest();

        request.setResourceId(resource.getExternalId());
        request.setFrom(
                String.valueOf(
                        happenedAfter.atZone(TimeUtil.BEIJING_ZONE_ID).toInstant().toEpochMilli()
                )
        );
        request.setTo(
                String.valueOf(
                        LocalDateTime.now().atZone(TimeUtil.BEIJING_ZONE_ID).toInstant().toEpochMilli()
                )
        );

        List<AlarmHistoryItemV2> histories = client.ces().describeAlarmHistories(request);

        List<ExternalAlertHistory> result = new ArrayList<>();

        for (AlarmHistoryItemV2 history : histories) {
            if(history.getAdditionalInfo() == null)
                continue;

            if(!Objects.equals(history.getAdditionalInfo().getResourceId(), resource.getExternalId()))
                continue;

            ExternalAlertHistory alertHistory = new ExternalAlertHistory(
                    history.getRecordId(),
                    convertLevel(history.getLevel()),
                    convertStatus(history.getStatus()),
                    history.getMetric().getMetricName(),
                    resource.getAccountId(),
                    resource.getCategory(),
                    resource.getExternalId(),
                    "%s%s告警: %s %s%s%s".formatted(
                            resource.getResourceHandler().getResourceTypeName(),
                            resource.getName(),
                            history.getMetric().getMetricName(),
                            history.getCondition().getFilter(),
                            history.getCondition().getComparisonOperator(),
                            history.getCondition().getValue()
                    ),
                    history.getFirstAlarmTime().toLocalDateTime(),
                    history.getLastAlarmTime().toLocalDateTime()
            );
            result.add(alertHistory);
        }

        return result;
    }

    @Override
    public Map<Metric, String> getShortMetricNames() {
        return Map.of(
                HuaweiMetrics.CPU_UTIL, "cpu",
                HuaweiMetrics.MEM_UTIL, "mem"
        );
    }

    private AlertStatus convertStatus(AlarmHistoryItemV2.StatusEnum status) {
        if(AlarmHistoryItemV2.StatusEnum.ALARM.equals(status))
            return AlertStatus.ALARM;
        else if(AlarmHistoryItemV2.StatusEnum.OK.equals(status))
            return AlertStatus.OK;
        else
            return AlertStatus.NO_CONF;
    }

    private StratoEventLevel convertLevel(AlarmHistoryItemV2.LevelEnum level) {
        if(AlarmHistoryItemV2.LevelEnum.NUMBER_1.equals(level))
            return StratoEventLevel.SERIOUS;
        else if(AlarmHistoryItemV2.LevelEnum.NUMBER_2.equals(level))
            return StratoEventLevel.WARNING;
        else if(AlarmHistoryItemV2.LevelEnum.NUMBER_3.equals(level))
            return StratoEventLevel.WARNING;
        else
            return StratoEventLevel.REMIND;
    }


    public static final SupportedMetric CPU_UTIL = new SupportedMetric(
            HuaweiMetrics.CPU_UTIL,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_CPU_USAGE),
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_CPU_USAGE_RECOVERED),
            true,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric CPU_USAGE_IDLE = new SupportedMetric(
            HuaweiMetrics.CPU_USAGE_IDLE,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric CPU_USAGE_USER = new SupportedMetric(
            HuaweiMetrics.CPU_USAGE_USER,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric CPU_USAGE_SYSTEM = new SupportedMetric(
            HuaweiMetrics.CPU_USAGE_SYSTEM,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric CPU_USAGE_OTHER = new SupportedMetric(
            HuaweiMetrics.CPU_USAGE_OTHER,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric CPU_USAGE_IOWAIT = new SupportedMetric(
            HuaweiMetrics.CPU_USAGE_IOWAIT,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric CPU_LOAD_AVERAGE_1 = new SupportedMetric(
            HuaweiMetrics.CPU_LOAD_AVERAGE_1,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric CPU_LOAD_AVERAGE_5 = new SupportedMetric(
            HuaweiMetrics.CPU_LOAD_AVERAGE_5,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric CPU_LOAD_AVERAGE_15 = new SupportedMetric(
            HuaweiMetrics.CPU_LOAD_AVERAGE_15,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric MEM_UTIL = new SupportedMetric(
            HuaweiMetrics.MEM_UTIL,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_MEMORY_USAGE),
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_MEMORY_USAGE_RECOVERED),
            true,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric DISK_READ_RATE = new SupportedMetric(
            HuaweiMetrics.DISK_READ_RATE,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric DISK_READ_IOPS = new SupportedMetric(
            HuaweiMetrics.DISK_READ_IOPS,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric DISK_WRITE_RATE = new SupportedMetric(
            HuaweiMetrics.DISK_WRITE_RATE,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric DISK_WRITE_IOPS = new SupportedMetric(
            HuaweiMetrics.DISK_WRITE_IOPS,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );


    public static final SupportedMetric NETWORK_BANDWIDTH_OUT = new SupportedMetric(
            HuaweiMetrics.NETWORK_BANDWIDTH_OUT,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric NETWORK_BANDWIDTH_IN = new SupportedMetric(
            HuaweiMetrics.NETWORK_BANDWIDTH_IN,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric NETWORK_PPS_OUT = new SupportedMetric(
            HuaweiMetrics.NETWORK_PPS_OUT,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric NETWORK_PPS_IN = new SupportedMetric(
            HuaweiMetrics.NETWORK_PPS_IN,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric NETWORK_NEW_CONNECTIONS = new SupportedMetric(
            HuaweiMetrics.NETWORK_NEW_CONNECTIONS,
            "instance_id",
            HuaweiMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
}
