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
            991
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
    public static final ResourceCategory INSTANCE_SNAPSHOT = new ResourceCategory(
            ResourceCategoryGroups.COMPUTE_INSTANCE_RELATED,
            "INSTANCE_SNAPSHOT",
            "云主机快照",
            "vm-snapshot",
            106
    );
    public static final ResourceCategory DISK_SNAPSHOT = new ResourceCategory(
            ResourceCategoryGroups.STORAGE_RELATED,
            "DISK_SNAPSHOT",
            "云硬盘快照",
            "disk-snapshot",
            107
    );

    public static final ResourceCategory DEPLOYMENT = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_WORKLOAD,
            "DEPLOYMENT",
            "Deployment",
            "deployment",
            61
    );

    public static final ResourceCategory STATEFUL_SET = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_WORKLOAD,
            "STATEFUL_SET",
            "StatefulSet",
            "stateful-set",
            62
    );

    public static final ResourceCategory DAEMON_SET = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_WORKLOAD,
            "DAEMON_SET",
            "DaemonSet",
            "daemon-set",
            63
    );

    public static final ResourceCategory POD = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_WORKLOAD,
            "POD",
            "Pod",
            "pod",
            64
    );

    public static final ResourceCategory CONTAINER_JOB = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_WORKLOAD,
            "CONTAINER_JOB",
            "Job",
            "container-job",
            65
    );

    public static final ResourceCategory CONTAINER_CRON_JOB = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_WORKLOAD,
            "CONTAINER_CRON_JOB",
            "CronJob",
            "container-cron-job",
            66
    );

    public static final ResourceCategory POD_VOLUME = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_STORAGE,
            "POD_VOLUME",
            "Volume",
            "pod-volume",
            71
    );

    public static final ResourceCategory PERSISTENT_VOLUME = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_STORAGE,
            "PERSISTENT_VOLUME",
            "PV",
            "pv",
            72
    );

    public static final ResourceCategory PERSISTENT_VOLUME_CLAIM = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_STORAGE,
            "PERSISTENT_VOLUME_CLAIM",
            "PVC",
            "pvc",
            73
    );



    public static final ResourceCategory STORAGE_CLASS = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_STORAGE,
            "STORAGE_CLASS",
            "StorageClass",
            "storage-class",
            74
    );

    public static final ResourceCategory CONFIG_MAP = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_STORAGE,
            "CONFIG_MAP",
            "ConfigMap",
            "config-map",
            75
    );

    public static final ResourceCategory SECRET = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_STORAGE,
            "SECRET",
            "Secret",
            "secret",
            76
    );

    public static final ResourceCategory SERVICE = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_NETWORK,
            "SERVICE",
            "Service",
            "service",
            81
    );

    public static final ResourceCategory ENDPOINT_SLICE = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_NETWORK,
            "ENDPOINT_SLICE",
            "EndpointSlice",
            "endpoint-slice",
            82
    );

    public static final ResourceCategory INGRESS = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_NETWORK,
            "INGRESS",
            "Ingress",
            "ingress",
            83
    );
    public static final ResourceCategory INGRESS_CLASS = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_NETWORK,
            "INGRESS_CLASS",
            "IngressClass",
            "ingress-class",
            84
    );



    public static final ResourceCategory NETWORK_POLICY = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_NETWORK,
            "NETWORK_POLICY",
            "NetworkPolicy",
            "network-policy",
            85
    );



    public static final ResourceCategory NAMESPACE = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_CLUSTER,
            "NAMESPACE",
            "Namespace",
            "namespace",
            86
    );

    public static final ResourceCategory NODE = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_CLUSTER,
            "NODE",
            "Node",
            "node",
            87
    );

    public static final ResourceCategory RUNTIME_CLASS = new ResourceCategory(
            ResourceCategoryGroups.CONTAINER_CLUSTER,
            "RUNTIME_CLASS",
            "RuntimeClass",
            "runtime-class",
            88
    );



}
