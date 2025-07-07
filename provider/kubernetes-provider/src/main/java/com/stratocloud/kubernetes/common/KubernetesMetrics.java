package com.stratocloud.kubernetes.common;

import com.stratocloud.resource.monitor.Metric;
import com.stratocloud.resource.monitor.MetricGroup;
import com.stratocloud.resource.monitor.MetricType;
import com.stratocloud.resource.monitor.MetricValueType;

import java.util.List;

public class KubernetesMetrics {
    public static final Metric NODE_CPU_UTIL = new Metric(
            "k8s_node",
            "cpu_util",
            "CPU使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(30)
    );

    public static final Metric NODE_MEM_UTIL = new Metric(
            "k8s_node",
            "mem_util",
            "内存使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.MEMORY_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(30)
    );


    public static final Metric POD_CPU_UTIL = new Metric(
            "k8s_pod",
            "cpu_util",
            "CPU使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.CPU_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(30)
    );

    public static final Metric POD_MEM_UTIL = new Metric(
            "k8s_pod",
            "mem_util",
            "内存使用率",
            "%",
            MetricType.TIME_SERIES,
            MetricGroup.MEMORY_METRICS,
            MetricValueType.VALUE,
            true,
            List.of(30)
    );
}
