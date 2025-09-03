package com.stratocloud.provider.tencent.metric;

import com.stratocloud.resource.monitor.Metric;
import com.stratocloud.resource.monitor.MetricGroup;
import com.stratocloud.resource.monitor.MetricType;
import com.stratocloud.resource.monitor.MetricValueType;

import java.util.List;
import java.util.Set;

public class TencentMetrics {
    public static final Metric CPU_USAGE = new Metric(
            "QCE/CVM",
            "CpuUsage",
            "CPU利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric CPU_LOAD_AVG = new Metric(
            "QCE/CVM",
            "CpuLoadavg",
            "CPU一分钟平均负载",
            "",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric CPU_LOAD_AVG_5M = new Metric(
            "QCE/CVM",
            "Cpuloadavg5m",
            "CPU五分钟平均负载",
            "",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(60, 300)
    );
    public static final Metric CPU_LOAD_AVG_15M = new Metric(
            "QCE/CVM",
            "Cpuloadavg15m",
            "CPU十五分钟平均负载",
            "",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(60, 300)
    );
    public static final Metric BASE_CPU_USAGE = new Metric(
            "QCE/CVM",
            "BaseCpuUsage",
            "基础CPU使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(10, 60, 300, 3600, 86400)
    );


    public static final Metric MEM_USED = new Metric(
            "QCE/CVM",
            "MemUsed",
            "内存使用量",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.MEMORY_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric MEM_USAGE = new Metric(
            "QCE/CVM",
            "MemUsage",
            "内存利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.MEMORY_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(10, 60, 300, 3600, 86400)
    );



    public static final Metric LAN_OUT_TRAFFIC = new Metric(
            "QCE/CVM",
            "LanOuttraffic",
            "内网出带宽",
            "Mbps",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric LAN_IN_TRAFFIC = new Metric(
            "QCE/CVM",
            "LanIntraffic",
            "内网入带宽",
            "Mbps",
            MetricType.TIME_SERIES,
            MetricGroup.MEMORY_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric LAN_OUT_PKG = new Metric(
            "QCE/CVM",
            "LanOutpkg",
            "内网出包量",
            "pkg/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric LAN_IN_PKG = new Metric(
            "QCE/CVM",
            "LanInpkg",
            "内网入包量",
            "pkg/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );

    public static final Metric WAN_OUT_TRAFFIC = new Metric(
            "QCE/CVM",
            "WanOuttraffic",
            "外网出带宽",
            "Mbps",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric WAN_IN_TRAFFIC = new Metric(
            "QCE/CVM",
            "WanIntraffic",
            "外网入带宽",
            "Mbps",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric WAN_OUT_PKG = new Metric(
            "QCE/CVM",
            "WanOutpkg",
            "外网出包量",
            "pkg/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric WAN_IN_PKG = new Metric(
            "QCE/CVM",
            "WanInpkg",
            "外网入包量",
            "pkg/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric OUT_RATIO = new Metric(
            "QCE/CVM",
            "Outratio",
            "公网出带宽利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(10, 60, 300)
    );


    public static final Metric DISK_TOTAL_USAGE = new Metric(
            "QCE/CVM",
            "CvmDiskUsage",
            "磁盘利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(60, 300, 3600, 86400)
    );
    public static final Metric DISK_READ_TRAFFIC = new Metric(
            "QCE/BLOCK_STORAGE",
            "DiskReadTraffic",
            "硬盘读流量",
            "KB/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric DISK_WRITE_TRAFFIC = new Metric(
            "QCE/BLOCK_STORAGE",
            "DiskWriteTraffic",
            "硬盘写流量",
            "KB/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric DISK_READ_IOPS = new Metric(
            "QCE/BLOCK_STORAGE",
            "DiskReadIops",
            "硬盘读IOPS",
            "",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric DISK_WRITE_IOPS = new Metric(
            "QCE/BLOCK_STORAGE",
            "DiskWriteIops",
            "硬盘写IOPS",
            "",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric DISK_AWAIT = new Metric(
            "QCE/BLOCK_STORAGE",
            "DiskAwait",
            "硬盘IO等待时间",
            "ms",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric DISK_SVCTM = new Metric(
            "QCE/BLOCK_STORAGE",
            "DiskSvctm",
            "硬盘IO服务时间",
            "ms",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );
    public static final Metric DISK_UTIL = new Metric(
            "QCE/BLOCK_STORAGE",
            "DiskUtil",
            "硬盘IO繁忙比率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );


    public static final Metric VIP_IN_TRAFFIC = new Metric(
            "QCE/LB",
            "VipIntraffic",
            "EIP入带宽",
            "Mbps",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );

    public static final Metric VIP_OUT_TRAFFIC = new Metric(
            "QCE/LB",
            "VipOuttraffic",
            "EIP入带宽",
            "Mbps",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            false,
            List.of(10, 60, 300, 3600, 86400)
    );

    public static final Metric BUCKET_STD_STORAGE = new Metric(
            "QCE/COS",
            "StdStorage",
            "标准存储容量",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(3600, 86400)
    );

    public static final Metric BUCKET_MAZ_STD_STORAGE = new Metric(
            "QCE/COS",
            "MazStdStorage",
            "多AZ标准存储容量",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(3600, 86400)
    );

    public static final Metric BUCKET_IA_STORAGE = new Metric(
            "QCE/COS",
            "SiaStorage",
            "低频访问存储容量",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(3600, 86400)
    );

    public static final Metric BUCKET_MAZ_IA_STORAGE = new Metric(
            "QCE/COS",
            "MazIaStorage",
            "多AZ低频访问存储容量",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(3600, 86400)
    );


    public static final Metric BUCKET_ARC_STORAGE = new Metric(
            "QCE/COS",
            "ArcStorage",
            "归档存储容量",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(3600, 86400)
    );

    public static final Metric BUCKET_MAZ_ARC_STORAGE = new Metric(
            "QCE/COS",
            "MazArcStorage",
            "多AZ归档存储容量",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(3600, 86400)
    );

    public static final Metric BUCKET_DEEP_ARC_STORAGE = new Metric(
            "QCE/COS",
            "DeepArcStorage",
            "深度归档存储容量",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(3600, 86400)
    );

    public static final Metric CDB_CPU_UTIL = new Metric(
            "QCE/CDB",
            "CpuUseRate",
            "CPU利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_MEMORY_UTIL = new Metric(
            "QCE/CDB",
            "MemoryUseRate",
            "内存利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_MEMORY_USE = new Metric(
            "QCE/CDB",
            "MemoryUse",
            "内存占用",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_DISK_UTIL = new Metric(
            "QCE/CDB",
            "VolumeRate",
            "磁盘利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_REAL_CAPACITY = new Metric(
            "QCE/CDB",
            "RealCapacity",
            "磁盘使用空间",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_CAPACITY = new Metric(
            "QCE/CDB",
            "Capacity",
            "磁盘占用空间",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_IOPS = new Metric(
            "QCE/CDB",
            "Iops",
            "IO 每秒请求量",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_IOPS_UTIL = new Metric(
            "QCE/CDB",
            "IopsUseRate",
            "IOPS 利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_BYTES_SENT = new Metric(
            "QCE/CDB",
            "BytesSent",
            "内网出流量",
            "Bytes/s",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_BYTES_RECEIVED = new Metric(
            "QCE/CDB",
            "BytesReceived",
            "内网入流量",
            "Bytes/s",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_QPS = new Metric(
            "QCE/CDB",
            "Qps",
            "每秒执行操作数",
            "times/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_TPS = new Metric(
            "QCE/CDB",
            "Tps",
            "每秒执行事务数",
            "times/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_CONNECTION_USE_RATE = new Metric(
            "QCE/CDB",
            "ConnectionUseRate",
            "连接数利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_MAX_CONNECTIONS = new Metric(
            "QCE/CDB",
            "MaxConnections",
            "最大连接数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_THREADS_CONNECTED = new Metric(
            "QCE/CDB",
            "ThreadsConnected",
            "当前打开连接数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_SLOW_QUERIES = new Metric(
            "QCE/CDB",
            "SlowQueries",
            "慢查询数",
            "count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_SELECT_SCAN = new Metric(
            "QCE/CDB",
            "SelectScan",
            "全表扫描数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_SELECT_COUNT = new Metric(
            "QCE/CDB",
            "SelectCount",
            "查询数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_COM_UPDATE = new Metric(
            "QCE/CDB",
            "ComUpdate",
            "更新数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_COM_DELETE = new Metric(
            "QCE/CDB",
            "ComDelete",
            "删除数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_COM_INSERT = new Metric(
            "QCE/CDB",
            "ComInsert",
            "插入数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_COM_REPLACE = new Metric(
            "QCE/CDB",
            "ComReplace",
            "覆盖数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_QUERIES = new Metric(
            "QCE/CDB",
            "Queries",
            "总请求数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_QUERY_RATE = new Metric(
            "QCE/CDB",
            "QueryRate",
            "查询使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_TMP_TABLES = new Metric(
            "QCE/CDB",
            "CreatedTmpTables",
            "临时表数量",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_TABLE_LOCKS_WAITED = new Metric(
            "QCE/CDB",
            "TableLocksWaited",
            "等待表锁次数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_INNODB_CACHE_HIT_RATE = new Metric(
            "QCE/CDB",
            "InnodbCacheHitRate",
            "缓存命中率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_INNODB_CACHE_USE_RATE = new Metric(
            "QCE/CDB",
            "InnodbCacheUseRate",
            "缓存使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_INNODB_OS_FILE_READS = new Metric(
            "QCE/CDB",
            "InnodbOsFileReads",
            "读磁盘数量",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_INNODB_OS_FILE_WRITES = new Metric(
            "QCE/CDB",
            "InnodbOsFileWrites",
            "写磁盘数量",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_INNODB_OS_FSYNCS = new Metric(
            "QCE/CDB",
            "InnodbOsFsyncs",
            "fsync数量",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_INNODB_NUM_OPEN_FILES = new Metric(
            "QCE/CDB",
            "InnodbNumOpenFiles",
            "当前InnoDB打开表的数量",
            "count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_KEY_CACHE_HIT_RATE = new Metric(
            "QCE/CDB",
            "KeyCacheHitRate",
            "缓存命中率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_KEY_CACHE_USE_RATE = new Metric(
            "QCE/CDB",
            "KeyCacheUseRate",
            "缓存使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_SLAVE_IO_RUNNING = new Metric(
            "QCE/CDB",
            "SlaveIoRunning",
            "IO线程状态",
            "0-Yes，1-No，2-Connecting",
            MetricType.TIME_SERIES,
            MetricGroup.DEPLOYMENT_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_SLAVE_SQL_RUNNING = new Metric(
            "QCE/CDB",
            "SlaveSqlRunning",
            "IO线程状态",
            "0-Yes，1-No",
            MetricType.TIME_SERIES,
            MetricGroup.DEPLOYMENT_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_MASTER_SLAVE_DISTANCE = new Metric(
            "QCE/CDB",
            "MasterSlaveSyncDistance",
            "主备延迟距离",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.DEPLOYMENT_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric CDB_SECONDS_BEHIND_MASTER = new Metric(
            "QCE/CDB",
            "SecondsBehindMaster",
            "主备延迟时间",
            "s",
            MetricType.TIME_SERIES,
            MetricGroup.DEPLOYMENT_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Set<Metric> CDB_METRICS = Set.of(
            CDB_CPU_UTIL,
            CDB_MEMORY_UTIL,
            CDB_MEMORY_USE,
            CDB_REAL_CAPACITY,
            CDB_DISK_UTIL,
            CDB_CAPACITY,
            CDB_IOPS,
            CDB_IOPS_UTIL,
            CDB_BYTES_SENT,
            CDB_BYTES_RECEIVED,
            CDB_QPS,
            CDB_TPS,
            CDB_CONNECTION_USE_RATE,
            CDB_MAX_CONNECTIONS,
            CDB_THREADS_CONNECTED,
            CDB_SLOW_QUERIES,
            CDB_SELECT_SCAN,
            CDB_SELECT_COUNT,
            CDB_COM_UPDATE,
            CDB_COM_DELETE,
            CDB_COM_REPLACE,
            CDB_COM_INSERT,
            CDB_QUERIES,
            CDB_QUERY_RATE,
            CDB_TMP_TABLES,
            CDB_TABLE_LOCKS_WAITED,

            CDB_INNODB_CACHE_HIT_RATE,
            CDB_INNODB_CACHE_USE_RATE,
            CDB_INNODB_OS_FILE_READS,
            CDB_INNODB_OS_FILE_WRITES,
            CDB_INNODB_OS_FSYNCS,
            CDB_INNODB_NUM_OPEN_FILES,
            CDB_KEY_CACHE_HIT_RATE,
            CDB_KEY_CACHE_USE_RATE,

            CDB_SLAVE_IO_RUNNING,
            CDB_SLAVE_SQL_RUNNING,
            CDB_MASTER_SLAVE_DISTANCE,
            CDB_SECONDS_BEHIND_MASTER
    );



    public static final Metric PG_CPU_UTIL = new Metric(
            "QCE/POSTGRES",
            "Cpu",
            "CPU利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300)
    );

    public static final Metric PG_MEMORY_UTIL = new Metric(
            "QCE/POSTGRES",
            "MemoryRate",
            "内存利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300)
    );

    public static final Metric PG_MEMORY_USE = new Metric(
            "QCE/POSTGRES",
            "Memory",
            "内存占用",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_REAL_CAPACITY = new Metric(
            "QCE/POSTGRES",
            "Storage",
            "已用存储空间",
            "GB",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_DISK_UTIL = new Metric(
            "QCE/POSTGRES",
            "StorageRate",
            "存储空间使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_QPS = new Metric(
            "QCE/POSTGRES",
            "Qps",
            "每秒查询数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_CONNECTIONS = new Metric(
            "QCE/POSTGRES",
            "Connections",
            "连接数",
            "count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_CALLS = new Metric(
            "QCE/POSTGRES",
            "ReadWriteCalls",
            "读写请求数",
            "count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_READ_CALLS = new Metric(
            "QCE/POSTGRES",
            "ReadCalls",
            "读请求数",
            "count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_WRITE_CALLS = new Metric(
            "QCE/POSTGRES",
            "WriteCalls",
            "写请求数",
            "count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_OTHER_CALLS = new Metric(
            "QCE/POSTGRES",
            "OtherCalls",
            "其他请求数",
            "count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_HIT_PERCENT = new Metric(
            "QCE/POSTGRES",
            "HitPercent",
            "缓冲区缓存命中率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );




    public static final Metric PG_SQL_RUNTIME_AVG = new Metric(
            "QCE/POSTGRES",
            "SqlRuntimeAvg",
            "平均执行时延",
            "ms",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_SQL_RUNTIME_MAX = new Metric(
            "QCE/POSTGRES",
            "SqlRuntimeMax",
            "最长TOP10执行时延",
            "ms",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_SQL_RUNTIME_MIN = new Metric(
            "QCE/POSTGRES",
            "SqlRuntimeMin",
            "最短TOP10执行时延",
            "ms",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_REMAIN_XID = new Metric(
            "QCE/POSTGRES",
            "RemainXid",
            "剩余XID数量",
            "count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_XLOG_DIFF = new Metric(
            "QCE/POSTGRES",
            "XlogDiff",
            "备库日志发送与回放位置差异",
            "Byte/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric PG_SLOW_QUERY_COUNT = new Metric(
            "QCE/POSTGRES",
            "SlowQueryCnt",
            "慢查询数量",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_FLUSH_LATENCY = new Metric(
            "QCE/POSTGRES",
            "FlushLatency",
            "备库日志落盘延迟",
            "Bytes",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(60, 300)
    );

    public static final Metric PG_XLOG_DIFF_TIME = new Metric(
            "QCE/POSTGRES",
            "XlogDiffTime",
            "备库日志落盘时间延迟",
            "s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_SLAVE_APPLY_DELAY = new Metric(
            "QCE/POSTGRES",
            "SlaveApplyDelay",
            "主备数据同步延迟",
            "Bytes",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_REPLAY_LAG = new Metric(
            "QCE/POSTGRES",
            "ReplayLag",
            "主备数据同步延迟时间",
            "s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(60, 300)
    );

    public static final Metric PG_ACTIVE_CONNS = new Metric(
            "QCE/POSTGRES",
            "ActiveConns",
            "活跃连接数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_IDLE_CONNS = new Metric(
            "QCE/POSTGRES",
            "IdleConns",
            "空闲连接数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_LONG_QUERY = new Metric(
            "QCE/POSTGRES",
            "LongQuery",
            "执行时长超过1秒的SQL数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_LONG_XACT = new Metric(
            "QCE/POSTGRES",
            "LongXact",
            "执行时长超过1秒的事务数目",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_IDLE_IN_XACT = new Metric(
            "QCE/POSTGRES",
            "IdleInXact",
            "空闲事务数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_LONG_IDLE_IN_XACT = new Metric(
            "QCE/POSTGRES",
            "LongIdleInXact",
            "超过5秒的空闲事务数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_WAITING = new Metric(
            "QCE/POSTGRES",
            "Waiting",
            "等待会话数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_LONG_WAITING = new Metric(
            "QCE/POSTGRES",
            "LongWaiting",
            "等待超过5s的会话数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_2PC = new Metric(
            "QCE/POSTGRES",
            "2pc",
            "2pc事务数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_LONG_2PC = new Metric(
            "QCE/POSTGRES",
            "Long2pc",
            "超过5s未提交的2pc事务数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_NEW_CONNS_IN_5S = new Metric(
            "QCE/POSTGRES",
            "NewConnIn5s",
            "5秒内新建连接数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_TPS = new Metric(
            "QCE/POSTGRES",
            "Tps",
            "每秒事务数",
            "Count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_XACT_COMMIT = new Metric(
            "QCE/POSTGRES",
            "XactCommit",
            "事务提交数",
            "Count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_XACT_ROLLBACK = new Metric(
            "QCE/POSTGRES",
            "XactRollback",
            "事务回滚数",
            "Count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_TUP_DELETED = new Metric(
            "QCE/POSTGRES",
            "TupDeleted",
            "每秒删除记录数",
            "Count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_TUP_INSERTED = new Metric(
            "QCE/POSTGRES",
            "TupInserted",
            "每秒插入记录数",
            "Count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_TUP_UPDATED = new Metric(
            "QCE/POSTGRES",
            "TupUpdated",
            "每秒更新记录数",
            "Count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_TUP_FETCHED = new Metric(
            "QCE/POSTGRES",
            "TupFetched",
            "每秒索引扫描记录数",
            "Count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );
    public static final Metric PG_TUP_RETURNED = new Metric(
            "QCE/POSTGRES",
            "TupReturned",
            "每秒全表扫描记录数",
            "Count/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_DEAD_LOCKS = new Metric(
            "QCE/POSTGRES",
            "Deadlocks",
            "死锁数",
            "Count",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_LOG_FILE_SIZE = new Metric(
            "QCE/POSTGRES",
            "LogFileSize",
            "日志文件大小",
            "KB",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_DATA_FILE_SIZE = new Metric(
            "QCE/POSTGRES",
            "DataFileSize",
            "数据文件大小",
            "KB",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_TEMP_FILE_SIZE = new Metric(
            "QCE/POSTGRES",
            "TempFileSize",
            "临时文件大小",
            "KB",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_THROUGHPUT = new Metric(
            "QCE/POSTGRES",
            "Throughput",
            "吞吐率",
            "KB/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_THROUGHPUT_READ = new Metric(
            "QCE/POSTGRES",
            "ThroughputRead",
            "读吞吐率",
            "KB/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_THROUGHPUT_WRITE = new Metric(
            "QCE/POSTGRES",
            "ThroughputWrite",
            "写吞吐率",
            "KB/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Metric PG_CONN_UTIL = new Metric(
            "QCE/POSTGRES",
            "ConnUtilization",
            "连接利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300)
    );


    public static final Metric PG_CLUSTER_IN_FLOW = new Metric(
            "QCE/POSTGRES",
            "ClusterInFlow",
            "网络入流量",
            "KB/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );


    public static final Metric PG_CLUSTER_OUT_FLOW = new Metric(
            "QCE/POSTGRES",
            "ClusterOutFlow",
            "网络出流量",
            "KB/s",
            MetricType.TIME_SERIES,
            MetricGroup.ENGINE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300)
    );

    public static final Set<Metric> PG_METRICS = Set.of(
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
            PG_CONN_UTIL, PG_CLUSTER_IN_FLOW, PG_CLUSTER_OUT_FLOW
    );


    public static final Metric REDIS_CPU_UTIL = new Metric(
            "QCE/REDIS_MEM",
            "CpuUtil",
            "CPU利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric REDIS_MEMORY_UTIL = new Metric(
            "QCE/REDIS_MEM",
            "MemUtil",
            "内存利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Metric REDIS_MEMORY_USE = new Metric(
            "QCE/REDIS_MEM",
            "MemUsed",
            "内存占用",
            "MB",
            MetricType.TIME_SERIES,
            MetricGroup.RESOURCE_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(5, 60, 300, 3600, 86400)
    );

    public static final Set<Metric> REDIS_METRICS = Set.of(
            REDIS_CPU_UTIL, REDIS_MEMORY_UTIL, REDIS_MEMORY_USE
    );
}
