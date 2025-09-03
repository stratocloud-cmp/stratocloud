package com.stratocloud.provider.tencent.redis.actions;

import com.stratocloud.form.*;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.redis.RedisType;
import lombok.Data;

import java.util.List;

@Data
public class TencentRedisBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费方式",
            options = {
                    "0", "1"
            },
            optionNames = {
                    "按量计费", "包年包月"
            },
            defaultValues = "0"
    )
    private Long billingMode;

    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                    "1年", "2年", "3年"
            },
            conditions = "this.billingMode === '1'",
            defaultValues = "1"
    )
    private Long period;
    @SelectField(
            label = "是否自动续费",
            options = {
                    "0",
                    "1"
            },
            optionNames = {
                    "手动续费",
                    "自动续费"
            },
            defaultValues = "0",
            conditions = "this.billingMode === '1'"
    )
    private Long autoRenewFlag;


    @SelectField(
            label = "产品版本",
            options = {
                    "Redis", "Memcached"
            },
            optionNames = {
                    "Redis版", "Memcached版"
            },
            defaultValues = "Redis"
    )
    private RedisType.Engine engine;
    @SelectField(
            label = "兼容版本",
            options = {
                    "7.0", "6.2", "5.0", "4.0"
            },
            optionNames = {
                    "7.0", "6.2", "5.0", "4.0"
            },
            defaultValues = "5.0",
            conditions = "this.engine === 'Redis'"
    )
    private String redisVersion;

    @SelectField(
            label = "兼容版本",
            options = {
                    "1.6"
            },
            optionNames = {
                    "1.6"
            },
            defaultValues = "1.6",
            conditions = "this.engine === 'Memcached'"
    )
    private String memcachedVersion;

    @SelectField(
            label = "架构版本",
            options = {
                    "Standard", "Cluster"
            },
            optionNames = {
                    "标准架构", "集群架构"
            },
            defaultValues = "Standard"
    )
    private RedisType.Architecture architecture;

    @SelectField(
            label = "内存容量",
            options = {
                    "256", "512", "1024", "2048", "4096", "6144", "8192", "10240", "12288",
                    "16384", "20480", "24576", "32768", "40960", "49152", "65536"
            },
            optionNames = {
                    "256MB", "512MB", "1GB", "2GB", "4GB", "6GB", "8GB", "10GB", "12GB",
                    "16GB", "20GB", "24GB", "32GB", "40GB", "48GB", "64GB"
            },
            defaultValues = "4096",
            conditions = "this.architecture === 'Standard'"
    )
    private Long standardMemoryMb;

    @SelectField(
            label = "分片数量",
            options = {
                    "1", "3", "5", "8", "12", "16", "24",
                    "32", "40", "48", "64", "80", "96", "128"
            },
            optionNames = {
                    "1片", "3片", "5片", "8片", "12片", "16片", "24片",
                    "32片", "40片", "48片", "64片", "80片", "96片", "128片"
            },
            defaultValues = "8",
            conditions = "this.architecture === 'Cluster'"
    )
    private Long shardNumber;

    @SelectField(
            label = "分片内存容量",
            options = {
                    "1024", "2048", "4096", "6144", "8192", "10240", "12288",
                    "16384", "20480", "24576", "32768", "40960", "49152", "65536"
            },
            optionNames = {
                    "1GB", "2GB", "4GB", "6GB", "8GB", "10GB", "12GB",
                    "16GB", "20GB", "24GB", "32GB", "40GB", "48GB", "64GB"
            },
            defaultValues = "4096",
            conditions = "this.architecture === 'Cluster'"
    )
    private Long shardMemoryMb;

    @NestedFormField(
            label = "副本节点",
            multiple = true,
            nestedFormClass = RedisReplicaNode.class,
            multipleMin = 1,
            multipleMax = 12
    )
    private List<RedisReplicaNode> replicaNodes;

    @BooleanField(label = "副本只读 (读写分离)")
    private boolean replicaReadOnly;

    @NumberField(label = "端口", defaultValue = 6379, min = 1024, max = 65535)
    private Long port;

    @BooleanField(label = "免密码访问")
    private boolean noAuth;
    @InputField(label = "密码", inputType = "password", conditions = "this.noAuth === false")
    private String password;
}
