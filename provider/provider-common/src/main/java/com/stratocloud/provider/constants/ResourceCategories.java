package com.stratocloud.provider.constants;

import com.stratocloud.resource.ResourceCategory;

public class ResourceCategories {
    public static final ResourceCategory COMPUTE_INSTANCE = new ResourceCategory(
            ResourceCategoryGroups.COMPUTE_INSTANCE_RELATED,
            "COMPUTE_INSTANCE",
            "云主机",
            "instance",
            0
    );

    public static final ResourceCategory DISK = new ResourceCategory(
            ResourceCategoryGroups.STORAGE_RELATED,
            "DISK",
            "云硬盘",
            "disk",
            1
    );

    public static final ResourceCategory NIC = new ResourceCategory(
            ResourceCategoryGroups.NETWORK_RELATED,
            "NIC",
            "弹性网卡",
            "nic",
            2
    );


    public static final ResourceCategory ELASTIC_IP = new ResourceCategory(
            ResourceCategoryGroups.NETWORK_RELATED,
            "ELASTIC_IP",
            "弹性IP",
            "eip",
            3
    );

    public static final ResourceCategory BANDWIDTH_PACKAGE = new ResourceCategory(
            ResourceCategoryGroups.NETWORK_RELATED,
            "BANDWIDTH_PACKAGE",
            "带宽包",
            "bwp",
            3
    );




    public static final ResourceCategory VPC = new ResourceCategory(
            ResourceCategoryGroups.NETWORK_RELATED,
            "VPC",
            "私有网络",
            "vpc",
            50
    );
    public static final ResourceCategory SUBNET = new ResourceCategory(
            ResourceCategoryGroups.NETWORK_RELATED,
            "SUBNET",
            "子网",
            "subnet",
            51
    );

    public static final ResourceCategory SECURITY_GROUP = new ResourceCategory(
            ResourceCategoryGroups.NETWORK_RELATED,
            "SECURITY_GROUP",
            "安全组",
            "sg",
            52
    );

    public static final ResourceCategory SECURITY_GROUP_INGRESS_POLICY = new ResourceCategory(
            ResourceCategoryGroups.NETWORK_RELATED,
            "SECURITY_GROUP_INGRESS_POLICY",
            "入站规则",
            "ingress-policy",
            53
    );

    public static final ResourceCategory SECURITY_GROUP_EGRESS_POLICY = new ResourceCategory(
            ResourceCategoryGroups.NETWORK_RELATED,
            "SECURITY_GROUP_EGRESS_POLICY",
            "出站规则",
            "egress-policy",
            54
    );



    public static final ResourceCategory ZONE = new ResourceCategory(
            ResourceCategoryGroups.COMPUTE_INSTANCE_RELATED,
            "ZONE",
            "可用区",
            "az",
            100
    );

    public static final ResourceCategory FLAVOR = new ResourceCategory(
            ResourceCategoryGroups.COMPUTE_INSTANCE_RELATED,
            "FLAVOR",
            "云主机规格",
            "flavor",
            101
    );
    public static final ResourceCategory IMAGE = new ResourceCategory(
            ResourceCategoryGroups.COMPUTE_INSTANCE_RELATED,
            "IMAGE",
            "镜像",
            "image",
            102
    );

    public static final ResourceCategory KEY_PAIR = new ResourceCategory(
            ResourceCategoryGroups.COMPUTE_INSTANCE_RELATED,
            "KEY_PAIR",
            "密钥对",
            "keypair",
            103
    );

    public static final ResourceCategory DISASTER_RECOVER_GROUP = new ResourceCategory(
            ResourceCategoryGroups.COMPUTE_INSTANCE_RELATED,
            "DISASTER_RECOVER_GROUP",
            "置放群组",
            "recover-group",
            104
    );

    public static final ResourceCategory CLUSTER = new ResourceCategory(
            ResourceCategoryGroups.COMPUTE_INSTANCE_RELATED,
            "CLUSTER",
            "集群",
            "cluster",
            104
    );

    public static final ResourceCategory HOST = new ResourceCategory(
            ResourceCategoryGroups.COMPUTE_INSTANCE_RELATED,
            "HOST",
            "宿主机",
            "host",
            105
    );


    public static final ResourceCategory LOAD_BALANCER = new ResourceCategory(
            ResourceCategoryGroups.LOAD_BALANCER_RELATED,
            "LOAD_BALANCER",
            "负载均衡实例",
            "lb",
            201
    );

    public static final ResourceCategory LOAD_BALANCER_LISTENER = new ResourceCategory(
            ResourceCategoryGroups.LOAD_BALANCER_RELATED,
            "LOAD_BALANCER_LISTENER",
            "监听器",
            "lb-listener",
            202
    );

    public static final ResourceCategory LOAD_BALANCER_RULE = new ResourceCategory(
            ResourceCategoryGroups.LOAD_BALANCER_RELATED,
            "LOAD_BALANCER_RULE",
            "转发规则",
            "lb-rule",
            203
    );

    public static final ResourceCategory LOAD_BALANCER_BACKEND = new ResourceCategory(
            ResourceCategoryGroups.LOAD_BALANCER_RELATED,
            "LOAD_BALANCER_BACKEND",
            "后端服务",
            "lb-backend",
            204
    );

    public static final ResourceCategory LOAD_BALANCER_BACKEND_GROUP = new ResourceCategory(
            ResourceCategoryGroups.LOAD_BALANCER_RELATED,
            "LOAD_BALANCER_BACKEND_GROUP",
            "后端服务器组",
            "lb-backend-group",
            204
    );

    public static final ResourceCategory SERVER_CERT = new ResourceCategory(
            ResourceCategoryGroups.OTHER,
            "SERVER_CERT",
            "服务器SSL证书",
            "server-cert",
            205
    );
    public static final ResourceCategory LOAD_BALANCER_ACL = new ResourceCategory(
            ResourceCategoryGroups.LOAD_BALANCER_RELATED,
            "LOAD_BALANCER_ACL",
            "ACL",
            "lb-acl",
            206
    );

    public static final ResourceCategory LB_FLAVOR = new ResourceCategory(
            ResourceCategoryGroups.LOAD_BALANCER_RELATED,
            "LB_FLAVOR",
            "LB规格",
            "lb-flavor",
            209
    );

    public static final ResourceCategory DISK_TYPE = new ResourceCategory(
            ResourceCategoryGroups.STORAGE_RELATED,
            "DISK_TYPE",
            "磁盘类型",
            "disk-type",
            105
    );

    public static final ResourceCategory DATASTORE = new ResourceCategory(
            ResourceCategoryGroups.STORAGE_RELATED,
            "DATASTORE",
            "数据存储",
            "datastore",
            106
    );

    public static final ResourceCategory LOAD_BALANCER_HEALTH_MONITOR = new ResourceCategory(
            ResourceCategoryGroups.LOAD_BALANCER_RELATED,
            "LOAD_BALANCER_HEALTH_MONITOR",
            "健康检查器",
            "lb-health-monitor",
            207
    );

    public static final ResourceCategory SOFTWARE = new ResourceCategory(
            ResourceCategoryGroups.DEVOPS_RELATED,
            "SOFTWARE",
            "软件",
            "software",
            301
    );

    public static final ResourceCategory INIT_SCRIPT = new ResourceCategory(
            ResourceCategoryGroups.DEVOPS_RELATED,
            "INIT_SCRIPT",
            "初始化脚本",
            "script",
            302
    );


    public static final ResourceCategory NON_CLOUD_MACHINE = new ResourceCategory(
            ResourceCategoryGroups.OTHER,
            "NON_CLOUD_MACHINE",
            "非云主机",
            "non-cloud-machine",
            999
    );

    public static final ResourceCategory CONTAINER = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_RELATED,
            "CONTAINER",
            "容器",
            "container",
            61
    );

    public static final ResourceCategory CLOUD_DATABASE = new ResourceCategory(
            ResourceCategoryGroups.CLOUD_DB_RELATED,
            "CLOUD_DATABASE",
            "云数据库实例",
            "cloud-db",
            251
    );

    public static final ResourceCategory CLOUD_MQ = new ResourceCategory(
            ResourceCategoryGroups.CLOUD_MQ_RELATED,
            "CLOUD_MQ",
            "云消息队列实例",
            "cloud-mq",
            261
    );
}
