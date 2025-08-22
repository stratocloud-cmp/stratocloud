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
            true,
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
            true,
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
            true,
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
            true,
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
            true,
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
            true,
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
            CDB_BYTES_RECEIVED
    );
}
