package com.stratocloud.provider.tencent.metric;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.provider.ResourceEventTypes;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.provider.resource.monitor.SupportedMetric;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentTimeUtil;
import com.stratocloud.provider.tencent.instance.TencentInstanceUtil;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.alert.AlertStatus;
import com.stratocloud.resource.alert.ExternalAlertHistory;
import com.stratocloud.resource.monitor.Metric;
import com.stratocloud.resource.monitor.MetricData;
import com.stratocloud.resource.monitor.MetricDataPoint;
import com.stratocloud.resource.monitor.*;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import com.tencentcloudapi.monitor.v20180724.models.*;
import com.tencentcloudapi.vpc.v20170312.models.Address;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Component
public class TencentMetricsProvider implements MetricsProvider {

    @Override
    public List<SupportedMetric> getSupportedMetrics() {
        return List.of(
                CPU_USAGE, CPU_LOAD_AVG, CPU_LOAD_AVG_5M, CPU_LOAD_AVG_15M, BASE_CPU_USAGE,
                MEM_USAGE, MEM_USED,
                DISK_TOTAL_USAGE, DISK_READ_TRAFFIC, DISK_WRITE_TRAFFIC, DISK_READ_IOPS, DISK_WRITE_IOPS,
                DISK_READ_TRAFFIC_FOR_DISK, DISK_WRITE_TRAFFIC_FOR_DISK,
                DISK_AWAIT, DISK_SVCTM, DISK_UTIL,
                OUT_RATIO,
                LAN_IN_TRAFFIC, LAN_OUT_TRAFFIC, LAN_IN_PKG, LAN_OUT_PKG,
                WAN_IN_TRAFFIC, WAN_OUT_TRAFFIC, WAN_IN_PKG, WAN_OUT_PKG,
                VIP_OUT_TRAFFIC, VIP_IN_TRAFFIC
        );
    }

    private static List<MetricObject> getInstanceMetricObjects(Resource resource){
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("InstanceId", resource.getExternalId())
                        )
                )
        );
    }


    private static List<MetricObject> getInstanceDisksMetricObjects(Resource resource){
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        TencentCloudClient client = getClient(resource);

        var instance = client.describeInstance(resource.getExternalId());

        if(instance.isEmpty())
            return List.of();

        List<String> diskIds = TencentInstanceUtil.getInstanceDiskIds(instance.get());

        return diskIds.stream().map(
                d -> new MetricObject(
                        List.of(
                                new MetricDimension("diskId", d)
                        )
                )
        ).toList();
    }

    private static List<MetricObject> getDiskMetricObjects(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        TencentCloudClient client = getClient(resource);

        Optional<Disk> disk = client.describeDisk(resource.getExternalId());

        if(disk.isEmpty())
            return List.of();

        if(disk.get().getAttached() == null || !disk.get().getAttached())
            return List.of();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("diskId", disk.get().getDiskId())
                        )
                )
        );
    }

    private static List<MetricObject> getEipMetricObjects(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        TencentCloudClient client = getClient(resource);
        Optional<Address> eip = client.describeEip(resource.getExternalId());
        if(eip.isEmpty())
            return List.of();

        String appId = client.getUserAppId().getAppId().toString();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("appId", appId),
                                new MetricDimension("eip", eip.get().getAddressIp())
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

        List<MetricObject> metricObjects = supportedMetric.objectsGetter().apply(resource);
        Metric metric = supportedMetric.metric();

        if(Utils.isEmpty(metricObjects))
            return new MetricData(metric, sequences);


        GetMonitorDataRequest request = new GetMonitorDataRequest();
        request.setNamespace(metric.metricNamespace());
        request.setMetricName(metric.metricName());
        request.setInstances(getInstancesArray(metricObjects));

        request.setPeriod((long) periodSeconds);
        request.setStartTime(TencentTimeUtil.fromLocalDateTime(from));
        request.setEndTime(TencentTimeUtil.fromLocalDateTime(to));

        GetMonitorDataResponse response = getClient(resource).getMonitorData(request);
        DataPoint[] dataPoints = response.getDataPoints();

        if(Utils.isNotEmpty(dataPoints)){
            for (DataPoint dataPoint : dataPoints) {
                if(Utils.isNotEmpty(dataPoint.getTimestamps()) && Utils.isNotEmpty(dataPoint.getValues())){
                    List<MetricDataPoint> points = new ArrayList<>();
                    for (int i = 0; i < dataPoint.getTimestamps().length; i++) {
                        Long timestamp = dataPoint.getTimestamps()[i];
                        Float value = dataPoint.getValues()[i];

                        if(timestamp != null && value != null)
                            points.add(
                                    new MetricDataPoint(
                                            value,
                                            TencentTimeUtil.fromEpochSeconds(timestamp)
                                    )
                            );
                    }
                    MetricSequence.of(
                            getDimensionValue(
                                    dataPoint.getDimensions(),
                                    supportedMetric.displayDimensionName()
                            ).orElse(metric.metricName()),
                            null,
                            points
                    ).ifPresent(sequences::add);
                }
            }
        }

        return new MetricData(supportedMetric.metric(), sequences);
    }

    @Override
    public Map<Metric, String> getShortMetricNames() {
        return Map.of(
                TencentMetrics.CPU_USAGE, "cpu",
                TencentMetrics.MEM_USAGE, "mem",
                TencentMetrics.DISK_READ_TRAFFIC, "r",
                TencentMetrics.DISK_WRITE_TRAFFIC, "w",
                TencentMetrics.VIP_IN_TRAFFIC, "in",
                TencentMetrics.VIP_OUT_TRAFFIC, "out"
        );
    }

    private static Optional<String> getDimensionValue(Dimension[] dimensions, String dimensionName){
        if(Utils.isEmpty(dimensions))
            return Optional.empty();

        return Arrays.stream(dimensions).filter(
                d -> Objects.equals(dimensionName, d.getName())
        ).map(Dimension::getValue).findAny();
    }

    private static TencentCloudClient getClient(Resource resource) {
        ResourceHandler resourceHandler = resource.getResourceHandler();
        TencentCloudProvider provider = (TencentCloudProvider) resourceHandler.getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(resource.getAccountId());
        return provider.buildClient(account);
    }

    private static Instance[] getInstancesArray(List<MetricObject> metricObjects) {
        List<Instance> instances = new ArrayList<>();

        for (MetricObject metricObject : metricObjects) {
            List<MetricDimension> metricDimensions = metricObject.dimensions();
            if(Utils.isNotEmpty(metricDimensions)){
                Instance instance = new Instance();
                List<Dimension> dimensions = new ArrayList<>();
                for (MetricDimension metricDimension : metricDimensions) {
                    Dimension dimension = new Dimension();
                    dimension.setName(metricDimension.name());
                    dimension.setValue(metricDimension.value());
                    dimensions.add(dimension);
                }
                instance.setDimensions(dimensions.toArray(Dimension[]::new));
                instances.add(instance);
            }
        }
        return instances.toArray(Instance[]::new);
    }

    @Override
    public List<ExternalAlertHistory> describeAlertHistories(Resource resource, LocalDateTime happenedAfter) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        TencentCloudClient client = getClient(resource);
        return client.describeAlarmHistories(resource.getExternalId(), happenedAfter).stream().map(
                h -> new ExternalAlertHistory(
                        h.getAlarmId(),
                        convertLevel(h.getAlarmLevel()),
                        convertStatus(h.getAlarmStatus()),
                        getMetricName(h.getMetricsInfo(), h.getMetricName()),
                        resource.getAccountId(),
                        resource.getCategory(),
                        resource.getExternalId(),
                        h.getContent(),
                        ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(h.getFirstOccurTime()),
                                TimeUtil.BEIJING_ZONE_ID
                        ).toLocalDateTime(),
                        ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(h.getLastOccurTime()),
                                TimeUtil.BEIJING_ZONE_ID
                        ).toLocalDateTime()
                )
        ).toList();
    }

    private String getMetricName(AlarmHistoryMetric[] metricsInfo, String defaultName) {
        if(Utils.isEmpty(metricsInfo))
            return defaultName;

        return metricsInfo[0].getMetricName();
    }

    private AlertStatus convertStatus(String alarmStatus) {
        return switch (alarmStatus){
            case "ALARM" -> AlertStatus.ALARM;
            case "OK" -> AlertStatus.OK;
            case "NO_CONF" -> AlertStatus.NO_CONF;
            default -> AlertStatus.NO_DATA;
        };
    }

    private StratoEventLevel convertLevel(String alarmLevel) {
        return switch (alarmLevel){
            case "Remind" -> StratoEventLevel.REMIND;
            case "Serious" -> StratoEventLevel.SERIOUS;
            default -> StratoEventLevel.WARNING;
        };
    }

    public static final SupportedMetric CPU_USAGE = new SupportedMetric(
            TencentMetrics.CPU_USAGE,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_CPU_USAGE),
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_CPU_USAGE_RECOVERED),
            true,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric CPU_LOAD_AVG = new SupportedMetric(
            TencentMetrics.CPU_LOAD_AVG,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric CPU_LOAD_AVG_5M = new SupportedMetric(
            TencentMetrics.CPU_LOAD_AVG_5M,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric CPU_LOAD_AVG_15M = new SupportedMetric(
            TencentMetrics.CPU_LOAD_AVG_15M,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric BASE_CPU_USAGE = new SupportedMetric(
            TencentMetrics.BASE_CPU_USAGE,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );


    public static final SupportedMetric MEM_USAGE = new SupportedMetric(
            TencentMetrics.MEM_USAGE,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_MEMORY_USAGE),
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_MEMORY_USAGE_RECOVERED),
            true,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric MEM_USED = new SupportedMetric(
            TencentMetrics.MEM_USED,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );


    public static final SupportedMetric DISK_TOTAL_USAGE = new SupportedMetric(
            TencentMetrics.DISK_TOTAL_USAGE,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_DISK_USAGE),
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_DISK_USAGE_RECOVERED),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_READ_TRAFFIC = new SupportedMetric(
            TencentMetrics.DISK_READ_TRAFFIC,
            "diskId",
            TencentMetricsProvider::getInstanceDisksMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_WRITE_TRAFFIC = new SupportedMetric(
            TencentMetrics.DISK_WRITE_TRAFFIC,
            "diskId",
            TencentMetricsProvider::getInstanceDisksMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );


    public static final SupportedMetric DISK_READ_TRAFFIC_FOR_DISK = new SupportedMetric(
            TencentMetrics.DISK_READ_TRAFFIC,
            "diskId",
            TencentMetricsProvider::getDiskMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.DISK
    );



    public static final SupportedMetric DISK_WRITE_TRAFFIC_FOR_DISK = new SupportedMetric(
            TencentMetrics.DISK_WRITE_TRAFFIC,
            "diskId",
            TencentMetricsProvider::getDiskMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.DISK
    );
    public static final SupportedMetric DISK_READ_IOPS = new SupportedMetric(
            TencentMetrics.DISK_READ_IOPS,
            "diskId",
            TencentMetricsProvider::getInstanceDisksMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_WRITE_IOPS = new SupportedMetric(
            TencentMetrics.DISK_WRITE_IOPS,
            "diskId",
            TencentMetricsProvider::getInstanceDisksMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_AWAIT = new SupportedMetric(
            TencentMetrics.DISK_AWAIT,
            "diskId",
            TencentMetricsProvider::getInstanceDisksMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_SVCTM = new SupportedMetric(
            TencentMetrics.DISK_SVCTM,
            "diskId",
            TencentMetricsProvider::getInstanceDisksMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric DISK_UTIL = new SupportedMetric(
            TencentMetrics.DISK_UTIL,
            "diskId",
            TencentMetricsProvider::getInstanceDisksMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );


    public static final SupportedMetric OUT_RATIO = new SupportedMetric(
            TencentMetrics.OUT_RATIO,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_BANDWIDTH_USAGE),
            Optional.of(ResourceEventTypes.INSTANCE_HIGH_BANDWIDTH_USAGE_RECOVERED),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric LAN_OUT_TRAFFIC = new SupportedMetric(
            TencentMetrics.LAN_OUT_TRAFFIC,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric LAN_IN_TRAFFIC = new SupportedMetric(
            TencentMetrics.LAN_IN_TRAFFIC,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric LAN_OUT_PKG = new SupportedMetric(
            TencentMetrics.LAN_OUT_PKG,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric LAN_IN_PKG = new SupportedMetric(
            TencentMetrics.LAN_IN_PKG,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric WAN_OUT_TRAFFIC = new SupportedMetric(
            TencentMetrics.WAN_OUT_TRAFFIC,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric WAN_IN_TRAFFIC = new SupportedMetric(
            TencentMetrics.WAN_IN_TRAFFIC,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric WAN_OUT_PKG = new SupportedMetric(
            TencentMetrics.WAN_OUT_PKG,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );
    public static final SupportedMetric WAN_IN_PKG = new SupportedMetric(
            TencentMetrics.WAN_IN_PKG,
            "InstanceId",
            TencentMetricsProvider::getInstanceMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.COMPUTE_INSTANCE
    );



    public static final SupportedMetric VIP_OUT_TRAFFIC = new SupportedMetric(
            TencentMetrics.VIP_OUT_TRAFFIC,
            "eip",
            TencentMetricsProvider::getEipMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.ELASTIC_IP
    );

    public static final SupportedMetric VIP_IN_TRAFFIC = new SupportedMetric(
            TencentMetrics.VIP_IN_TRAFFIC,
            "eip",
            TencentMetricsProvider::getEipMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.ELASTIC_IP
    );
}
