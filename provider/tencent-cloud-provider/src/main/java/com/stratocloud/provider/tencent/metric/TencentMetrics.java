package com.stratocloud.provider.tencent.metric;

import com.stratocloud.resource.monitor.Metric;
import com.stratocloud.resource.monitor.MetricGroup;
import com.stratocloud.resource.monitor.MetricType;
import com.stratocloud.resource.monitor.MetricValueType;

import java.util.List;

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
            List.of(5, 10, 60, 300, 3600, 86400)
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
            List.of(5, 10, 60, 300, 3600, 86400)
    );
}
