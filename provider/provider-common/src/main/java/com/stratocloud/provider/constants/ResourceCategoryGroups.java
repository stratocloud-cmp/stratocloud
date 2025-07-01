package com.stratocloud.provider.constants;

import com.stratocloud.resource.ResourceCategoryGroup;

public class ResourceCategoryGroups {
    public static final ResourceCategoryGroup COMPUTE_INSTANCE_RELATED = new ResourceCategoryGroup(
            "COMPUTE_INSTANCE_RELATED", "云主机相关"
    );

    public static final ResourceCategoryGroup STORAGE_RELATED = new ResourceCategoryGroup(
            "STORAGE_RELATED", "存储"
    );

    public static final ResourceCategoryGroup NETWORK_RELATED = new ResourceCategoryGroup(
            "NETWORK_RELATED", "网络"
    );

    public static final ResourceCategoryGroup CONTAINER_WORKLOAD = new ResourceCategoryGroup(
            "CONTAINER_WORKLOAD", "容器工作负载"
    );

    public static final ResourceCategoryGroup CONTAINER_STORAGE = new ResourceCategoryGroup(
            "CONTAINER_STORAGE", "容器存储"
    );

    public static final ResourceCategoryGroup CONTAINER_NETWORK = new ResourceCategoryGroup(
            "CONTAINER_NETWORK", "容器网络"
    );

    public static final ResourceCategoryGroup CONTAINER_CLUSTER = new ResourceCategoryGroup(
            "CONTAINER_CLUSTER", "容器集群"
    );

    public static final ResourceCategoryGroup LOAD_BALANCER_RELATED = new ResourceCategoryGroup(
            "LOAD_BALANCER_RELATED", "负载均衡"
    );

    public static final ResourceCategoryGroup CLOUD_DB_RELATED = new ResourceCategoryGroup(
            "CLOUD_DB_RELATED", "云数据库"
    );

    public static final ResourceCategoryGroup CLOUD_MQ_RELATED = new ResourceCategoryGroup(
            "CLOUD_MQ_RELATED", "云消息队列"
    );

    public static final ResourceCategoryGroup DEVOPS_RELATED = new ResourceCategoryGroup(
            "DEVOPS_RELATED", "运维自动化"
    );

    public static final ResourceCategoryGroup OTHER = new ResourceCategoryGroup(
            "OTHER", "其他"
    );



}
