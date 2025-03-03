package com.stratocloud.provider.constants;

import com.stratocloud.resource.ResourceUsageType;

public class UsageTypes {
    public static final ResourceUsageType CPU_CORES = new ResourceUsageType("CPU_CORES", "处理器核数");
    public static final ResourceUsageType MEMORY_GB = new ResourceUsageType("MEMORY_GB", "内存使用量(GB)");
    public static final ResourceUsageType DISK_GB = new ResourceUsageType("DISK_GB", "硬盘使用量(GB)");
    public static final ResourceUsageType NIC_IP = new ResourceUsageType("NIC_IP", "网卡IP数量");
    public static final ResourceUsageType ELASTIC_IP = new ResourceUsageType("ELASTIC_IP", "弹性IP数量");
}
