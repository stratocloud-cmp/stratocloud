package com.stratocloud.resource.monitor;

public record MetricGroup(String id, String name) {
    public static final MetricGroup CPU_METRICS = new MetricGroup(
            "CpuMetrics", "CPU监控"
    );

    public static final MetricGroup MEMORY_METRICS = new MetricGroup(
            "MemoryMetrics", "内存监控"
    );

    public static final MetricGroup STORAGE_METRICS = new MetricGroup(
            "StorageMetrics", "存储监控"
    );

    public static final MetricGroup NETWORK_METRICS = new MetricGroup(
            "NetworkMetrics", "网络监控"
    );

    public static final MetricGroup RESOURCE_METRICS = new MetricGroup(
            "ResourceMetrics", "资源监控"
    );

    public static final MetricGroup ENGINE_METRICS = new MetricGroup(
            "EngineMetrics", "引擎监控"
    );

    public static final MetricGroup DEPLOYMENT_METRICS = new MetricGroup(
            "DeploymentMetrics", "部署监控"
    );
}
