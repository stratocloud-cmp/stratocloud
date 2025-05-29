package com.stratocloud.provider.huawei.metrics;

import com.stratocloud.resource.monitor.Metric;
import com.stratocloud.resource.monitor.MetricGroup;
import com.stratocloud.resource.monitor.MetricType;
import com.stratocloud.resource.monitor.MetricValueType;

import java.util.List;

public class HuaweiMetrics {
    public static final Metric CPU_UTIL = new Metric(
            "SYS.ECS",
            "cpu_util",
            "CPU使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );

    public static final Metric CPU_USAGE_IDLE = new Metric(
            "AGT.ECS",
            "cpu_usage_idle",
            "CPU空闲时间占比",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_USAGE_USER = new Metric(
            "AGT.ECS",
            "cpu_usage_user",
            "用户空间CPU使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_USAGE_SYSTEM = new Metric(
            "AGT.ECS",
            "cpu_usage_system",
            "内核空间CPU使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_USAGE_OTHER = new Metric(
            "AGT.ECS",
            "cpu_usage_other",
            "其他CPU使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_USAGE_IOWAIT = new Metric(
            "AGT.ECS",
            "cpu_usage_iowait",
            "iowait状态占比",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_LOAD_AVERAGE_1 = new Metric(
            "AGT.ECS",
            "load_average1",
            "1分钟平均负载",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_LOAD_AVERAGE_5 = new Metric(
            "AGT.ECS",
            "load_average5",
            "5分钟平均负载",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_LOAD_AVERAGE_15 = new Metric(
            "AGT.ECS",
            "load_average15",
            "15分钟平均负载",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );



    public static final Metric MEM_UTIL = new Metric(
            "AGT.ECS",
            "mem_usedPercent",
            "内存使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.MEMORY_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric DISK_READ_RATE = new Metric(
            "SYS.ECS",
            "disk_read_bytes_rate",
            "磁盘读带宽",
            "B/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );

    public static final Metric DISK_WRITE_RATE = new Metric(
            "SYS.ECS",
            "disk_write_bytes_rate",
            "磁盘写带宽",
            "B/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );

    public static final Metric DISK_READ_IOPS = new Metric(
            "SYS.ECS",
            "disk_read_requests_rate",
            "磁盘读IOPS",
            "request/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );

    public static final Metric DISK_WRITE_IOPS = new Metric(
            "SYS.ECS",
            "disk_write_requests_rate",
            "磁盘写IOPS",
            "request/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );


    public static final Metric NETWORK_BANDWIDTH_IN = new Metric(
            "SYS.ECS",
            "network_vm_bandwidth_in",
            "虚拟机入方向带宽",
            "B/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );

    public static final Metric NETWORK_BANDWIDTH_OUT = new Metric(
            "SYS.ECS",
            "network_vm_bandwidth_out",
            "虚拟机出方向带宽",
            "B/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );

    public static final Metric NETWORK_PPS_IN = new Metric(
            "SYS.ECS",
            "network_vm_pps_in",
            "虚拟机入方向PPS",
            "packet/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );

    public static final Metric NETWORK_PPS_OUT = new Metric(
            "SYS.ECS",
            "network_vm_pps_out",
            "虚拟机出方向PPS",
            "packet/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );

    public static final Metric NETWORK_NEW_CONNECTIONS = new Metric(
            "SYS.ECS",
            "network_vm_newconnections",
            "虚拟机整机新建连接数",
            "connect/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            true,
            List.of(300)
    );
}
