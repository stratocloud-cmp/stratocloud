package com.stratocloud.provider.tencent.metric;

import com.qcloud.cos.model.HeadBucketResult;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.provider.ResourceEventTypes;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.provider.resource.monitor.SupportedMetric;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentCloudRegion;
import com.stratocloud.provider.tencent.common.TencentTimeUtil;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceUtil;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
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
import com.tencentcloudapi.cdb.v20170320.models.InstanceInfo;
import com.tencentcloudapi.monitor.v20180724.models.*;
import com.tencentcloudapi.vpc.v20170312.models.Address;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

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
                VIP_OUT_TRAFFIC, VIP_IN_TRAFFIC,
                BUCKET_STD_STORAGE, BUCKET_MAZ_STD_STORAGE, BUCKET_IA_STORAGE, BUCKET_MAZ_IA_STORAGE,
                BUCKET_ARC_STORAGE, BUCKET_MAZ_ARC_STORAGE, BUCKET_DEEP_ARC_STORAGE,
                CDB_CPU_UTIL, CDB_MEMORY_UTIL, CDB_MEMORY_USE, CDB_DISK_UTIL,
                CDB_REAL_CAPACITY, CDB_CAPACITY, CDB_IOPS, CDB_IOPS_UTIL, CDB_BYTES_SENT, CDB_BYTES_RECEIVED,

                CDB_QPS, CDB_TPS, CDB_CONNECTION_USE_RATE, CDB_MAX_CONNECTIONS, CDB_THREADS_CONNECTED, CDB_SLOW_QUERIES,
                CDB_SELECT_SCAN, CDB_SELECT_COUNT, CDB_COM_UPDATE, CDB_COM_DELETE, CDB_COM_REPLACE, CDB_COM_INSERT,
                CDB_QUERIES, CDB_QUERY_RATE, CDB_TMP_TABLES, CDB_TABLE_LOCKS_WAITED,

                CDB_INNODB_CACHE_HIT_RATE, CDB_INNODB_CACHE_USE_RATE, CDB_INNODB_OS_FILE_READS, CDB_INNODB_OS_FILE_WRITES,
                CDB_INNODB_OS_FSYNCS, CDB_INNODB_NUM_OPEN_FILES, CDB_KEY_CACHE_HIT_RATE, CDB_KEY_CACHE_USE_RATE,

                CDB_SLAVE_IO_RUNNING, CDB_SLAVE_SQL_RUNNING, CDB_MASTER_SLAVE_DISTANCE, CDB_SECONDS_BEHIND_MASTER,

                PG_CPU_UTIL, PG_MEMORY_UTIL, PG_DISK_UTIL, PG_MEMORY_USE, PG_REAL_CAPACITY,
                PG_QPS, PG_CONNECTIONS, PG_CALLS, PG_READ_CALLS, PG_WRITE_CALLS, PG_OTHER_CALLS,
                PG_HIT_PERCENT, PG_SQL_RUNTIME_AVG, PG_SQL_RUNTIME_MAX, PG_SQL_RUNTIME_MIN,
                PG_REMAIN_XID, PG_XLOG_DIFF, PG_SLOW_QUERY_COUNT, PG_FLUSH_LATENCY,
                PG_XLOG_DIFF_TIME, PG_SLAVE_APPLY_DELAY, PG_REPLAY_LAG, PG_ACTIVE_CONNS,
                PG_IDLE_CONNS, PG_LONG_QUERY, PG_LONG_XACT, PG_IDLE_IN_XACT, PG_LONG_IDLE_IN_XACT,
                PG_WAITING, PG_LONG_WAITING, PG_2PC, PG_LONG_2PC, PG_NEW_CONNS_IN_5S,
                PG_TPS, PG_XACT_COMMIT, PG_XACT_ROLLBACK, PG_TUP_DELETED, PG_TUP_INSERTED,
                PG_TUP_UPDATED, PG_TUP_FETCHED, PG_TUP_RETURNED, PG_DEAD_LOCKS,
                PG_LOG_FILE_SIZE, PG_DATA_FILE_SIZE, PG_TEMP_FILE_SIZE,
                PG_THROUGHPUT, PG_THROUGHPUT_READ, PG_THROUGHPUT_WRITE,
                PG_CONN_UTIL, PG_CLUSTER_IN_FLOW, PG_CLUSTER_OUT_FLOW,

                REDIS_CPU_UTIL, REDIS_MEMORY_UTIL, REDIS_MEMORY_USE
        );
    }

    @Override
    public List<SupportedMetric> getSupportedMetrics(Resource resource) {
        if(Objects.equals(ResourceCategories.BUCKET.id(), resource.getCategory())){
            CosSessionKey sessionKey = getClient(resource).getCosSessionKey();
            CosSession cosSession = CosSessionManager.getSession(sessionKey);

            Optional<HeadBucketResult> headBucketResult = cosSession.headBucket(resource.getExternalId());

            if(headBucketResult.isPresent()){
                boolean mazBucket = headBucketResult.get().isMazBucket();
                if(mazBucket)
                    return List.of(BUCKET_MAZ_STD_STORAGE, BUCKET_MAZ_IA_STORAGE, BUCKET_MAZ_ARC_STORAGE);
                else
                    return List.of(BUCKET_STD_STORAGE, BUCKET_IA_STORAGE, BUCKET_ARC_STORAGE, BUCKET_DEEP_ARC_STORAGE);
            }else {
                return List.of();
            }
        }

        if(resource.getResourceHandler() instanceof TencentCdbHandler){
            return MetricsProvider.super.getSupportedMetrics(resource).stream().filter(
                    m -> TencentMetrics.CDB_METRICS.contains(m.metric())
            ).toList();
        }

        if(resource.getResourceHandler() instanceof TencentPgHandler){
            return MetricsProvider.super.getSupportedMetrics(resource).stream().filter(
                    m -> TencentMetrics.PG_METRICS.contains(m.metric())
            ).toList();
        }

        if(resource.getResourceHandler() instanceof TencentRedisHandler){
            return MetricsProvider.super.getSupportedMetrics(resource).stream().filter(
                    m -> TencentMetrics.REDIS_METRICS.contains(m.metric())
            ).toList();
        }

        return MetricsProvider.super.getSupportedMetrics(resource);
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

    private static List<MetricObject> getCdbMetricObjects(Resource resource){
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        TencentCloudClient client = getClient(resource);
        Optional<InstanceInfo> cdb = client.describeCdbInstance(resource.getExternalId());

        if(cdb.isEmpty())
            return List.of();

        return Stream.of("1", "2", "3", "4").map(
                instanceType -> new MetricObject(
                        List.of(
                                new MetricDimension("InstanceId", resource.getExternalId()),
                                new MetricDimension("InstanceType", instanceType)
                        )
                )
        ).toList();
    }

    private static List<MetricObject> getPgMetricObjects(Resource resource){
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("resourceId", resource.getExternalId())
                        )
                )
        );
    }

    private static List<MetricObject> getRedisMetricObjects(Resource resource){
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("instanceid", resource.getExternalId())
                        )
                )
        );
    }

    private static List<MetricObject> getBucketMetricObjects(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return List.of();

        return List.of(
                new MetricObject(
                        List.of(
                                new MetricDimension("bucket", resource.getExternalId())
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

        GetMonitorDataResponse response = getClient(resource, true).getMonitorData(request);
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
                            ).map(
                                    v -> translateDimensionValue(supportedMetric, v)
                            ).orElse(metric.metricName()),
                            null,
                            points
                    ).ifPresent(sequences::add);
                }
            }
        }

        return new MetricData(supportedMetric.metric(), sequences);
    }

    private String translateDimensionValue(SupportedMetric supportedMetric, String value) {
        if(TencentMetrics.CDB_METRICS.contains(supportedMetric.metric())){
            if(Utils.isBlank(value))
                return value;

            return switch (value){
                case "1" -> "主机";
                case "2" -> "从机";
                case "3" -> "只读实例";
                case "4" -> "第二从机";
                default -> value;
            };
        } else {
            return value;
        }
    }

    @Override
    public Map<Metric, String> getShortMetricNames() {
        return Map.ofEntries(
                Map.entry(TencentMetrics.CPU_USAGE, "cpu"),
                Map.entry(TencentMetrics.MEM_USAGE, "mem"),
                Map.entry(TencentMetrics.DISK_READ_TRAFFIC, "r"),
                Map.entry(TencentMetrics.DISK_WRITE_TRAFFIC, "w"),
                Map.entry(TencentMetrics.VIP_IN_TRAFFIC, "in"),
                Map.entry(TencentMetrics.VIP_OUT_TRAFFIC, "out"),
                Map.entry(TencentMetrics.CDB_CPU_UTIL, "cpu"),
                Map.entry(TencentMetrics.CDB_MEMORY_UTIL, "mem"),
                Map.entry(TencentMetrics.CDB_DISK_UTIL, "disk"),
                Map.entry(TencentMetrics.PG_CPU_UTIL, "cpu"),
                Map.entry(TencentMetrics.PG_MEMORY_UTIL, "mem"),
                Map.entry(TencentMetrics.PG_DISK_UTIL, "disk"),
                Map.entry(TencentMetrics.REDIS_CPU_UTIL, "cpu"),
                Map.entry(TencentMetrics.REDIS_MEMORY_UTIL, "mem")
        );
    }


    private static Optional<String> getDimensionValue(Dimension[] dimensions, String dimensionName){
        if(Utils.isEmpty(dimensions))
            return Optional.empty();

        return Arrays.stream(dimensions).filter(
                d -> Objects.equals(dimensionName, d.getName())
        ).map(Dimension::getValue).findAny();
    }

    private static TencentCloudClient getClient(Resource resource, boolean specifiedBucketMetricsRegion) {
        ResourceHandler resourceHandler = resource.getResourceHandler();
        TencentCloudProvider provider = (TencentCloudProvider) resourceHandler.getProvider();
        ExternalAccount account = provider.getAccountRepository().findExternalAccount(resource.getAccountId());

        if(specifiedBucketMetricsRegion && ResourceCategories.BUCKET.id().equals(resource.getCategory())){
            return provider.buildClientWithRegion(account, TencentCloudRegion.GUANGZHOU);
        }

        return provider.buildClient(account);
    }

    private static TencentCloudClient getClient(Resource resource) {
        return getClient(resource, false);
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

    public static final SupportedMetric BUCKET_STD_STORAGE = new SupportedMetric(
            TencentMetrics.BUCKET_STD_STORAGE,
            "bucket",
            TencentMetricsProvider::getBucketMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.BUCKET
    );

    public static final SupportedMetric BUCKET_MAZ_STD_STORAGE = new SupportedMetric(
            TencentMetrics.BUCKET_MAZ_STD_STORAGE,
            "bucket",
            TencentMetricsProvider::getBucketMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.BUCKET
    );

    public static final SupportedMetric BUCKET_IA_STORAGE = new SupportedMetric(
            TencentMetrics.BUCKET_IA_STORAGE,
            "bucket",
            TencentMetricsProvider::getBucketMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.BUCKET
    );

    public static final SupportedMetric BUCKET_MAZ_IA_STORAGE = new SupportedMetric(
            TencentMetrics.BUCKET_MAZ_IA_STORAGE,
            "bucket",
            TencentMetricsProvider::getBucketMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.BUCKET
    );

    public static final SupportedMetric BUCKET_ARC_STORAGE = new SupportedMetric(
            TencentMetrics.BUCKET_ARC_STORAGE,
            "bucket",
            TencentMetricsProvider::getBucketMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.BUCKET
    );

    public static final SupportedMetric BUCKET_MAZ_ARC_STORAGE = new SupportedMetric(
            TencentMetrics.BUCKET_MAZ_ARC_STORAGE,
            "bucket",
            TencentMetricsProvider::getBucketMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.BUCKET
    );

    public static final SupportedMetric BUCKET_DEEP_ARC_STORAGE = new SupportedMetric(
            TencentMetrics.BUCKET_DEEP_ARC_STORAGE,
            "bucket",
            TencentMetricsProvider::getBucketMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.BUCKET
    );


    public static final SupportedMetric CDB_CPU_UTIL = new SupportedMetric(
            TencentMetrics.CDB_CPU_UTIL,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_MEMORY_UTIL = new SupportedMetric(
            TencentMetrics.CDB_MEMORY_UTIL,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_MEMORY_USE = new SupportedMetric(
            TencentMetrics.CDB_MEMORY_USE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_DISK_UTIL = new SupportedMetric(
            TencentMetrics.CDB_DISK_UTIL,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_REAL_CAPACITY = new SupportedMetric(
            TencentMetrics.CDB_REAL_CAPACITY,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_CAPACITY = new SupportedMetric(
            TencentMetrics.CDB_CAPACITY,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_IOPS = new SupportedMetric(
            TencentMetrics.CDB_IOPS,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_IOPS_UTIL = new SupportedMetric(
            TencentMetrics.CDB_IOPS_UTIL,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );



    public static final SupportedMetric CDB_BYTES_SENT = new SupportedMetric(
            TencentMetrics.CDB_BYTES_SENT,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_BYTES_RECEIVED = new SupportedMetric(
            TencentMetrics.CDB_BYTES_RECEIVED,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_QPS = new SupportedMetric(
            TencentMetrics.CDB_QPS,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_TPS = new SupportedMetric(
            TencentMetrics.CDB_TPS,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_CONNECTION_USE_RATE = new SupportedMetric(
            TencentMetrics.CDB_CONNECTION_USE_RATE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_MAX_CONNECTIONS = new SupportedMetric(
            TencentMetrics.CDB_MAX_CONNECTIONS,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_THREADS_CONNECTED = new SupportedMetric(
            TencentMetrics.CDB_THREADS_CONNECTED,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_SLOW_QUERIES = new SupportedMetric(
            TencentMetrics.CDB_SLOW_QUERIES,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_SELECT_SCAN = new SupportedMetric(
            TencentMetrics.CDB_SELECT_SCAN,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_SELECT_COUNT = new SupportedMetric(
            TencentMetrics.CDB_SELECT_COUNT,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_COM_UPDATE = new SupportedMetric(
            TencentMetrics.CDB_COM_UPDATE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_COM_DELETE = new SupportedMetric(
            TencentMetrics.CDB_COM_DELETE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_COM_INSERT = new SupportedMetric(
            TencentMetrics.CDB_COM_INSERT,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_COM_REPLACE = new SupportedMetric(
            TencentMetrics.CDB_COM_REPLACE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_QUERIES = new SupportedMetric(
            TencentMetrics.CDB_QUERIES,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_QUERY_RATE = new SupportedMetric(
            TencentMetrics.CDB_QUERY_RATE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_TMP_TABLES = new SupportedMetric(
            TencentMetrics.CDB_TMP_TABLES,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric CDB_TABLE_LOCKS_WAITED = new SupportedMetric(
            TencentMetrics.CDB_TABLE_LOCKS_WAITED,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );


    public static final SupportedMetric CDB_INNODB_CACHE_HIT_RATE = new SupportedMetric(
            TencentMetrics.CDB_INNODB_CACHE_HIT_RATE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_INNODB_CACHE_USE_RATE = new SupportedMetric(
            TencentMetrics.CDB_INNODB_CACHE_USE_RATE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_INNODB_OS_FILE_READS = new SupportedMetric(
            TencentMetrics.CDB_INNODB_OS_FILE_READS,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_INNODB_OS_FILE_WRITES = new SupportedMetric(
            TencentMetrics.CDB_INNODB_OS_FILE_WRITES,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_INNODB_OS_FSYNCS = new SupportedMetric(
            TencentMetrics.CDB_INNODB_OS_FSYNCS,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_INNODB_NUM_OPEN_FILES = new SupportedMetric(
            TencentMetrics.CDB_INNODB_NUM_OPEN_FILES,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_KEY_CACHE_HIT_RATE = new SupportedMetric(
            TencentMetrics.CDB_KEY_CACHE_HIT_RATE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_KEY_CACHE_USE_RATE = new SupportedMetric(
            TencentMetrics.CDB_KEY_CACHE_USE_RATE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );


    public static final SupportedMetric CDB_SLAVE_IO_RUNNING = new SupportedMetric(
            TencentMetrics.CDB_SLAVE_IO_RUNNING,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_SLAVE_SQL_RUNNING = new SupportedMetric(
            TencentMetrics.CDB_SLAVE_SQL_RUNNING,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_MASTER_SLAVE_DISTANCE = new SupportedMetric(
            TencentMetrics.CDB_MASTER_SLAVE_DISTANCE,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric CDB_SECONDS_BEHIND_MASTER = new SupportedMetric(
            TencentMetrics.CDB_SECONDS_BEHIND_MASTER,
            "InstanceType",
            TencentMetricsProvider::getCdbMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );



    public static final SupportedMetric PG_CPU_UTIL = new SupportedMetric(
            TencentMetrics.PG_CPU_UTIL,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric PG_MEMORY_UTIL = new SupportedMetric(
            TencentMetrics.PG_MEMORY_UTIL,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric PG_DISK_UTIL = new SupportedMetric(
            TencentMetrics.PG_DISK_UTIL,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );

    public static final SupportedMetric PG_MEMORY_USE = new SupportedMetric(
            TencentMetrics.PG_MEMORY_USE,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_REAL_CAPACITY = new SupportedMetric(
            TencentMetrics.PG_REAL_CAPACITY,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_QPS = new SupportedMetric(
            TencentMetrics.PG_QPS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_CONNECTIONS = new SupportedMetric(
            TencentMetrics.PG_CONNECTIONS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_CALLS = new SupportedMetric(
            TencentMetrics.PG_CALLS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_READ_CALLS = new SupportedMetric(
            TencentMetrics.PG_READ_CALLS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_WRITE_CALLS = new SupportedMetric(
            TencentMetrics.PG_WRITE_CALLS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_OTHER_CALLS = new SupportedMetric(
            TencentMetrics.PG_OTHER_CALLS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_HIT_PERCENT = new SupportedMetric(
            TencentMetrics.PG_HIT_PERCENT,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_SQL_RUNTIME_AVG = new SupportedMetric(
            TencentMetrics.PG_SQL_RUNTIME_AVG,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_SQL_RUNTIME_MAX = new SupportedMetric(
            TencentMetrics.PG_SQL_RUNTIME_MAX,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_SQL_RUNTIME_MIN = new SupportedMetric(
            TencentMetrics.PG_SQL_RUNTIME_MIN,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_REMAIN_XID = new SupportedMetric(
            TencentMetrics.PG_REMAIN_XID,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_XLOG_DIFF = new SupportedMetric(
            TencentMetrics.PG_XLOG_DIFF,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_SLOW_QUERY_COUNT = new SupportedMetric(
            TencentMetrics.PG_SLOW_QUERY_COUNT,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_FLUSH_LATENCY = new SupportedMetric(
            TencentMetrics.PG_FLUSH_LATENCY,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_XLOG_DIFF_TIME = new SupportedMetric(
            TencentMetrics.PG_XLOG_DIFF_TIME,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_SLAVE_APPLY_DELAY = new SupportedMetric(
            TencentMetrics.PG_SLAVE_APPLY_DELAY,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_REPLAY_LAG = new SupportedMetric(
            TencentMetrics.PG_REPLAY_LAG,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_ACTIVE_CONNS = new SupportedMetric(
            TencentMetrics.PG_ACTIVE_CONNS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_IDLE_CONNS = new SupportedMetric(
            TencentMetrics.PG_IDLE_CONNS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_LONG_QUERY = new SupportedMetric(
            TencentMetrics.PG_LONG_QUERY,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_LONG_XACT = new SupportedMetric(
            TencentMetrics.PG_LONG_XACT,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_IDLE_IN_XACT = new SupportedMetric(
            TencentMetrics.PG_IDLE_IN_XACT,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_LONG_IDLE_IN_XACT = new SupportedMetric(
            TencentMetrics.PG_LONG_IDLE_IN_XACT,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_WAITING = new SupportedMetric(
            TencentMetrics.PG_WAITING,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_LONG_WAITING = new SupportedMetric(
            TencentMetrics.PG_LONG_WAITING,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_2PC = new SupportedMetric(
            TencentMetrics.PG_2PC,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_LONG_2PC = new SupportedMetric(
            TencentMetrics.PG_LONG_2PC,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_NEW_CONNS_IN_5S = new SupportedMetric(
            TencentMetrics.PG_NEW_CONNS_IN_5S,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_TPS = new SupportedMetric(
            TencentMetrics.PG_TPS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_XACT_COMMIT = new SupportedMetric(
            TencentMetrics.PG_XACT_COMMIT,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_XACT_ROLLBACK = new SupportedMetric(
            TencentMetrics.PG_XACT_ROLLBACK,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_TUP_DELETED = new SupportedMetric(
            TencentMetrics.PG_TUP_DELETED,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_TUP_INSERTED = new SupportedMetric(
            TencentMetrics.PG_TUP_INSERTED,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_TUP_UPDATED = new SupportedMetric(
            TencentMetrics.PG_TUP_UPDATED,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_TUP_FETCHED = new SupportedMetric(
            TencentMetrics.PG_TUP_FETCHED,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_TUP_RETURNED = new SupportedMetric(
            TencentMetrics.PG_TUP_RETURNED,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_DEAD_LOCKS = new SupportedMetric(
            TencentMetrics.PG_DEAD_LOCKS,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_LOG_FILE_SIZE = new SupportedMetric(
            TencentMetrics.PG_LOG_FILE_SIZE,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_DATA_FILE_SIZE = new SupportedMetric(
            TencentMetrics.PG_DATA_FILE_SIZE,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_TEMP_FILE_SIZE = new SupportedMetric(
            TencentMetrics.PG_TEMP_FILE_SIZE,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_THROUGHPUT = new SupportedMetric(
            TencentMetrics.PG_THROUGHPUT,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_THROUGHPUT_READ = new SupportedMetric(
            TencentMetrics.PG_THROUGHPUT_READ,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_THROUGHPUT_WRITE = new SupportedMetric(
            TencentMetrics.PG_THROUGHPUT_WRITE,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_CONN_UTIL = new SupportedMetric(
            TencentMetrics.PG_CONN_UTIL,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_CLUSTER_IN_FLOW = new SupportedMetric(
            TencentMetrics.PG_CLUSTER_IN_FLOW,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );
    public static final SupportedMetric PG_CLUSTER_OUT_FLOW = new SupportedMetric(
            TencentMetrics.PG_CLUSTER_OUT_FLOW,
            "resourceId",
            TencentMetricsProvider::getPgMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.RELATIONAL_DB_INSTANCE
    );


    public static final SupportedMetric REDIS_CPU_UTIL = new SupportedMetric(
            TencentMetrics.REDIS_CPU_UTIL,
            "instanceid",
            TencentMetricsProvider::getRedisMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.NOSQL_DB_INSTANCE
    );

    public static final SupportedMetric REDIS_MEMORY_UTIL = new SupportedMetric(
            TencentMetrics.REDIS_MEMORY_UTIL,
            "instanceid",
            TencentMetricsProvider::getRedisMetricObjects,
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.NOSQL_DB_INSTANCE
    );

    public static final SupportedMetric REDIS_MEMORY_USE = new SupportedMetric(
            TencentMetrics.REDIS_MEMORY_USE,
            "instanceid",
            TencentMetricsProvider::getRedisMetricObjects,
            Optional.empty(),
            Optional.empty(),
            false,
            ResourceCategories.NOSQL_DB_INSTANCE
    );
}
