package com.stratocloud.provider.aliyun.metric;

import com.stratocloud.resource.monitor.Metric;
import com.stratocloud.resource.monitor.MetricGroup;
import com.stratocloud.resource.monitor.MetricType;
import com.stratocloud.resource.monitor.MetricValueType;

import java.util.List;

public class AliyunMetrics {
    public static final Metric CPU_UTIL = new Metric(
            "acs_ecs_dashboard",
            "CPUUtilization",
            "CPU利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_IDLE = new Metric(
            "acs_ecs_dashboard",
            "cpu_idle",
            "空闲CPU占用",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_SYSTEM = new Metric(
            "acs_ecs_dashboard",
            "cpu_system",
            "内核空间CPU占用",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_USER = new Metric(
            "acs_ecs_dashboard",
            "cpu_user",
            "用户空间CPU占用",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_OTHER = new Metric(
            "acs_ecs_dashboard",
            "cpu_other",
            "其他CPU占用",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric CPU_WAIT = new Metric(
            "acs_ecs_dashboard",
            "cpu_wait",
            "等待IO操作CPU占用",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );




    public static final Metric MEMORY_UTIL = new Metric(
            "acs_ecs_dashboard",
            "memory_usedutilization",
            "内存利用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.MEMORY_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );

    public static final Metric MEMORY_USED = new Metric(
            "acs_ecs_dashboard",
            "memory_usedspace",
            "内存使用率",
            "B",
            MetricType.TIME_SERIES,
            MetricGroup.MEMORY_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );

    public static final Metric PER_DISK_READ_BPS = new Metric(
            "acs_ecs_dashboard",
            "disk_readbytes",
            "各磁盘读取BPS",
            "B/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );

    public static final Metric PER_DISK_WRITE_BPS = new Metric(
            "acs_ecs_dashboard",
            "disk_writebytes",
            "各磁盘写入BPS",
            "B/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );

    public static final Metric PER_DISK_UTIL = new Metric(
            "acs_ecs_dashboard",
            "diskusage_utilization",
            "磁盘使用率",
            "B/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            true,
            List.of(60)
    );


    public static final Metric DISK_READ_BPS = new Metric(
            "acs_ecs_dashboard",
            "DiskReadBPS",
            "所有磁盘读取BPS",
            "B/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );
    public static final Metric DISK_READ_BPS_UTIL = new Metric(
            "acs_ecs_dashboard",
            "DiskReadBPSUtilization",
            "所有磁盘读取BPS使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(60)
    );
    public static final Metric DISK_READ_IOPS = new Metric(
            "acs_ecs_dashboard",
            "DiskReadIOPS",
            "所有磁盘每秒读取次数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );
    public static final Metric DISK_READ_IOPS_UTIL = new Metric(
            "acs_ecs_dashboard",
            "DiskReadIOPSUtilization",
            "所有磁盘读取IOPS使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(60)
    );


    public static final Metric DISK_WRITE_BPS = new Metric(
            "acs_ecs_dashboard",
            "DiskWriteBPS",
            "所有磁盘写入BPS",
            "B/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );
    public static final Metric DISK_WRITE_BPS_UTIL = new Metric(
            "acs_ecs_dashboard",
            "DiskWriteBPSUtilization",
            "所有磁盘写入BPS使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(60)
    );
    public static final Metric DISK_WRITE_IOPS = new Metric(
            "acs_ecs_dashboard",
            "DiskWriteIOPS",
            "所有磁盘每秒写入次数",
            "count/s",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );
    public static final Metric DISK_WRITE_IOPS_UTIL = new Metric(
            "acs_ecs_dashboard",
            "DiskWriteIOPSUtilization",
            "所有磁盘每秒写入次数使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.STORAGE_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(60)
    );



    public static final Metric INTRANET_IN_RATE = new Metric(
            "acs_ecs_dashboard",
            "IntranetInRate",
            "内网流入带宽",
            "bit/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );
    public static final Metric INTRANET_IN_RATE_UTIL = new Metric(
            "acs_ecs_dashboard",
            "IntranetInRateUtilization",
            "内网流入带宽使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(60)
    );
    public static final Metric INTRANET_OUT_RATE = new Metric(
            "acs_ecs_dashboard",
            "IntranetOutRate",
            "内网流出带宽",
            "bit/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );
    public static final Metric INTRANET_OUT_RATE_UTIL = new Metric(
            "acs_ecs_dashboard",
            "IntranetOutRateUtilization",
            "内网流出带宽使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(60)
    );


    public static final Metric ECS_EIP_IN_RATE = new Metric(
            "acs_ecs_dashboard",
            "eip_InternetInRate",
            "EIP流入带宽",
            "bit/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(60)
    );
    public static final Metric ECS_EIP_OUT_RATE = new Metric(
            "acs_ecs_dashboard",
            "eip_InternetOutRate",
            "EIP流出带宽",
            "bit/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.VALUE,
            false,
            List.of(60)
    );


    public static final Metric EIP_IN_RATE = new Metric(
            "acs_vpc_eip",
            "net_rx.rate",
            "EIP流入带宽",
            "bit/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );
    public static final Metric EIP_OUT_RATE = new Metric(
            "acs_vpc_eip",
            "net_tx.rate",
            "EIP流出带宽",
            "bit/s",
            MetricType.TIME_SERIES,
            MetricGroup.NETWORK_METRICS,
            MetricValueType.AVG,
            false,
            List.of(60)
    );
}
