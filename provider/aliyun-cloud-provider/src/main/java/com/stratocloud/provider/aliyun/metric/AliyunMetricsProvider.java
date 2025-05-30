package com.stratocloud.provider.aliyun.metric;

import com.aliyun.cms20190101.models.DescribeAlertLogListRequest;
import com.aliyun.cms20190101.models.DescribeAlertLogListResponseBody;
import com.aliyun.cms20190101.models.DescribeMetricListRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.provider.ResourceEventTypes;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.AliyunMetricDataPoint;
import com.stratocloud.provider.aliyun.disk.AliyunDisk;
import com.stratocloud.provider.aliyun.eip.AliyunEip;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.provider.resource.monitor.SupportedMetric;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.alert.AlertStatus;
import com.stratocloud.resource.alert.ExternalAlertHistory;
import com.stratocloud.resource.monitor.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AliyunMetricsProvider implements MetricsProvider {
    @Override
    public List<SupportedMetric> getSupportedMetrics() {
        return List.of(
                CPU_UTIL,CPU_IDLE,CPU_SYSTEM,CPU_USER,CPU_OTHER,CPU_WAIT,
                MEMORY_UTIL,MEMORY_USED,
                PER_DISK_UTIL, PER_DISK_READ_BPS, PER_DISK_WRITE_BPS,
                INSTANCE_PER_DISK_UTIL, INSTANCE_PER_DISK_READ_BPS, INSTANCE_PER_DISK_WRITE_BPS,
                DISK_READ_BPS,DISK_READ_BPS_UTIL,DISK_READ_IOPS,DISK_READ_IOPS_UTIL,
                DISK_WRITE_BPS,DISK_WRITE_BPS_UTIL,DISK_WRITE_IOPS,DISK_WRITE_IOPS_UTIL,
                INTRANET_IN_RATE,INTRANET_IN_RATE_UTIL,INTRANET_OUT_RATE,INTRANET_OUT_RATE_UTIL,
                ECS_EIP_OUT_RATE,ECS_EIP_IN_RATE,EIP_OUT_RATE,EIP_IN_RATE
        );
    }

    private static List<MetricObject> getInstanceMetricObjects(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("instanceId", resource.getExternalId())
                        )
                )
        );
    }

    private static List<MetricObject> getDiskMetricObjects(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        Optional<AliyunDisk> disk = getClient(resource).ecs().describeDisk(resource.getExternalId());

        if(disk.isEmpty())
            return List.of();

        String instanceId = disk.get().detail().getInstanceId();
        if(Utils.isBlank(instanceId))
            return List.of();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("instanceId", instanceId)
                        )
                )
        );
    }

    private static List<MetricObject> getInstanceDiskMetricsObjects(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("instanceId", resource.getExternalId())
                        )
                )
        );
    }

    private static List<MetricObject> getEipMetricObjects(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("instanceId", resource.getExternalId())
                        )
                )
        );
    }

    private static List<MetricObject> getInstanceEipMetricObjects(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        AliyunClient client = getClient(resource);

        List<AliyunEip> eips = client.vpc().describeEipsByAssociatedInstanceId(resource.getExternalId());

        return eips.stream().map(
                eip -> new MetricObject(
                        List.of(
                                new MetricDimension("instanceId", eip.detail().getAllocationId())
                        )
                )
        ).toList();
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


        DescribeMetricListRequest request = new DescribeMetricListRequest();
        request.setNamespace(metric.metricNamespace());
        request.setMetricName(metric.metricName());

        List<Map<String, String>> dimensions = new ArrayList<>();

        for (MetricObject metricObject : metricObjects) {
            if(Utils.isEmpty(metricObject.dimensions()))
                continue;

            for (MetricDimension metricDimension : metricObject.dimensions()) {
                dimensions.add(Map.of(metricDimension.name(), metricDimension.value()));
            }

        }

        request.setLength("1440");
        request.setDimensions(JSON.toJsonString(dimensions));
        request.setPeriod(String.valueOf(periodSeconds));
        request.setStartTime(String.valueOf(from.atZone(TimeUtil.BEIJING_ZONE_ID).toInstant().toEpochMilli()));
        request.setEndTime(String.valueOf(to.atZone(TimeUtil.BEIJING_ZONE_ID).toInstant().toEpochMilli()));

        List<AliyunMetricDataPoint> dataPoints = getClient(resource).cms().describeMetricList(request);

        if(Utils.isNotEmpty(dataPoints)) {
            Map<String, List<AliyunMetricDataPoint>> sequencesMap
                    = dataPoints.stream().collect(Collectors.groupingBy(AliyunMetricsProvider::getSequenceName));

            for (String sequenceName : sequencesMap.keySet()) {
                List<AliyunMetricDataPoint> sequencePoints = sequencesMap.get(sequenceName);

                if (Utils.isEmpty(sequencePoints))
                    continue;

                List<MetricDataPoint> points = new ArrayList<>();

                for (AliyunMetricDataPoint sequencePoint : sequencePoints) {
                    Float value = getMetricValue(metric, sequencePoint);
                    Long timestamp = sequencePoint.timestamp();
                    if (timestamp == null || value == null)
                        continue;
                    points.add(
                            new MetricDataPoint(
                                    value,
                                    LocalDateTime.ofInstant(
                                            Instant.ofEpochMilli(
                                                    timestamp
                                            ),
                                            TimeUtil.BEIJING_ZONE_ID
                                    )
                            )
                    );
                }

                MetricSequence.of(
                        sequenceName,
                        null,
                        points
                ).ifPresent(sequences::add);
            }
        }

        return new MetricData(metric, sequences);
    }

    private static String getSequenceName(AliyunMetricDataPoint p) {
        return Utils.isNotBlank(p.device()) ? p.device() : p.instanceId();
    }

    @Override
    public Map<Metric, String> getShortMetricNames() {
        return Map.of(
                AliyunMetrics.CPU_UTIL, "cpu",
                AliyunMetrics.MEMORY_UTIL, "mem",
                AliyunMetrics.EIP_IN_RATE, "in",
                AliyunMetrics.EIP_OUT_RATE, "out",
                AliyunMetrics.PER_DISK_READ_BPS, "r",
                AliyunMetrics.PER_DISK_WRITE_BPS, "w"
        );
    }

    private Float getMetricValue(Metric metric, AliyunMetricDataPoint sequencePoint) {
        return switch (metric.metricValueType()){
            case VALUE -> sequencePoint.Value();
            case AVG -> sequencePoint.Average();
            case MAX -> sequencePoint.Maximum();
            case MIN -> sequencePoint.Minimum();
        };
    }

    private static AliyunClient getClient(Resource resource) {
        AliyunCloudProvider provider = (AliyunCloudProvider) resource.getResourceHandler().getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(resource.getAccountId());
        return provider.buildClient(account);
    }

    @Override
    public List<ExternalAlertHistory> describeAlertHistories(Resource resource, LocalDateTime happenedAfter) {
        AliyunClient client = getClient(resource);

        DescribeAlertLogListRequest request = new DescribeAlertLogListRequest();

        request.setSearchKey(resource.getExternalId());
        request.setStartTime(happenedAfter.atZone(TimeUtil.BEIJING_ZONE_ID).toInstant().toEpochMilli());
        request.setEndTime(System.currentTimeMillis());

        var alertLogs = client.cms().describeAlertHistories(request);

        List<ExternalAlertHistory> result = new ArrayList<>();

        if(Utils.isNotEmpty(alertLogs)){
            for (var alertLog : alertLogs) {
                if(!Objects.equals(alertLog.getInstanceId(), resource.getExternalId()))
                    continue;

                ExternalAlertHistory history = new ExternalAlertHistory(
                        alertLog.getLogId(),
                        convertLevel(alertLog),
                        convertStatus(alertLog),
                        alertLog.getMetricName(),
                        resource.getAccountId(),
                        resource.getCategory(),
                        resource.getExternalId(),
                        "%s%s告警: %s %s".formatted(
                                resource.getResourceHandler().getResourceTypeName(),
                                resource.getName(),
                                alertLog.getMetricName(),
                                alertLog.getEscalation().getExpression()
                        ),
                        ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(Long.parseLong(alertLog.getAlertTime())),
                                TimeUtil.BEIJING_ZONE_ID
                        ).toLocalDateTime(),
                        ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(Long.parseLong(alertLog.getAlertTime())),
                                TimeUtil.BEIJING_ZONE_ID
                        ).toLocalDateTime()
                );
                result.add(history);
            }
        }

        return result;
    }

    private AlertStatus convertStatus(DescribeAlertLogListResponseBody.DescribeAlertLogListResponseBodyAlertLogList alertLog) {
        if("OK".equals(alertLog.getLevel()))
            return AlertStatus.OK;
        return "0".equals(alertLog.getSendStatus()) ? AlertStatus.ALARM : AlertStatus.NO_CONF;
    }

    private StratoEventLevel convertLevel(DescribeAlertLogListResponseBody.DescribeAlertLogListResponseBodyAlertLogList alertLog) {
        return "0".equals(alertLog.getSendStatus()) ? StratoEventLevel.WARNING : StratoEventLevel.REMIND;
    }


    public static final SupportedMetric CPU_UTIL = new SupportedMetric(
            AliyunMetrics.CPU_UTIL,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_CPU_USAGE),
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_CPU_USAGE_RECOVERED),
            true,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric CPU_IDLE = new SupportedMetric(
            AliyunMetrics.CPU_IDLE,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric CPU_SYSTEM = new SupportedMetric(
            AliyunMetrics.CPU_SYSTEM,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric CPU_USER = new SupportedMetric(
            AliyunMetrics.CPU_USER,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric CPU_OTHER = new SupportedMetric(
            AliyunMetrics.CPU_OTHER,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric CPU_WAIT = new SupportedMetric(
            AliyunMetrics.CPU_WAIT,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );



    public static final SupportedMetric MEMORY_UTIL = new SupportedMetric(
            AliyunMetrics.MEMORY_UTIL,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_MEMORY_USAGE),
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_MEMORY_USAGE_RECOVERED),
            true,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric MEMORY_USED = new SupportedMetric(
            AliyunMetrics.MEMORY_USED,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );


    public static final SupportedMetric PER_DISK_READ_BPS = new SupportedMetric(
            AliyunMetrics.PER_DISK_READ_BPS,
            "device",
            AliyunMetricsProvider::getDiskMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.DISK
    );

    public static final SupportedMetric PER_DISK_WRITE_BPS = new SupportedMetric(
            AliyunMetrics.PER_DISK_WRITE_BPS,
            "device",
            AliyunMetricsProvider::getDiskMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.DISK
    );



    public static final SupportedMetric PER_DISK_UTIL = new SupportedMetric(
            AliyunMetrics.PER_DISK_UTIL,
            "device",
            AliyunMetricsProvider::getDiskMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.DISK
    );

    public static final SupportedMetric INSTANCE_PER_DISK_READ_BPS = new SupportedMetric(
            AliyunMetrics.PER_DISK_READ_BPS,
            "device",
            AliyunMetricsProvider::getInstanceDiskMetricsObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric INSTANCE_PER_DISK_WRITE_BPS = new SupportedMetric(
            AliyunMetrics.PER_DISK_WRITE_BPS,
            "device",
            AliyunMetricsProvider::getInstanceDiskMetricsObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );

    public static final SupportedMetric INSTANCE_PER_DISK_UTIL = new SupportedMetric(
            AliyunMetrics.PER_DISK_UTIL,
            "device",
            AliyunMetricsProvider::getInstanceDiskMetricsObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );


    public static final SupportedMetric DISK_READ_BPS = new SupportedMetric(
            AliyunMetrics.DISK_READ_BPS,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_READ_BPS_UTIL = new SupportedMetric(
            AliyunMetrics.DISK_READ_BPS_UTIL,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_READ_IOPS = new SupportedMetric(
            AliyunMetrics.DISK_READ_IOPS,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_READ_IOPS_UTIL = new SupportedMetric(
            AliyunMetrics.DISK_READ_IOPS_UTIL,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_WRITE_BPS = new SupportedMetric(
            AliyunMetrics.DISK_WRITE_BPS,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_WRITE_BPS_UTIL = new SupportedMetric(
            AliyunMetrics.DISK_WRITE_BPS_UTIL,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_WRITE_IOPS = new SupportedMetric(
            AliyunMetrics.DISK_WRITE_IOPS,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_WRITE_IOPS_UTIL = new SupportedMetric(
            AliyunMetrics.DISK_WRITE_IOPS_UTIL,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );




    public static final SupportedMetric INTRANET_IN_RATE = new SupportedMetric(
            AliyunMetrics.INTRANET_IN_RATE,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric INTRANET_IN_RATE_UTIL = new SupportedMetric(
            AliyunMetrics.INTRANET_IN_RATE_UTIL,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric INTRANET_OUT_RATE = new SupportedMetric(
            AliyunMetrics.INTRANET_OUT_RATE,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric INTRANET_OUT_RATE_UTIL = new SupportedMetric(
            AliyunMetrics.INTRANET_OUT_RATE_UTIL,
            "instanceId",
            AliyunMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric ECS_EIP_IN_RATE = new SupportedMetric(
            AliyunMetrics.ECS_EIP_IN_RATE,
            "instanceId",
            AliyunMetricsProvider::getInstanceEipMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );



    public static final SupportedMetric ECS_EIP_OUT_RATE = new SupportedMetric(
            AliyunMetrics.ECS_EIP_OUT_RATE,
            "instanceId",
            AliyunMetricsProvider::getInstanceEipMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric EIP_IN_RATE = new SupportedMetric(
            AliyunMetrics.EIP_IN_RATE,
            "instanceId",
            AliyunMetricsProvider::getEipMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.ELASTIC_IP
    );



    public static final SupportedMetric EIP_OUT_RATE = new SupportedMetric(
            AliyunMetrics.EIP_OUT_RATE,
            "instanceId",
            AliyunMetricsProvider::getEipMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.ELASTIC_IP
    );
}
