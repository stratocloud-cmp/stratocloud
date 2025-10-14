package com.stratocloud.provider.aliyun.redis.actions;

import com.stratocloud.form.*;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunRedisBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费方式",
            options = {
                    "PrePaid",
                    "PostPaid"
            },
            optionNames = {
                    "包年包月",
                    "按量计费"
            },
            defaultValues = "PostPaid"
    )
    public String chargeType;

    @BooleanField(label = "自动使用代金券")
    public boolean autoUseCoupon;

    @NestedFormField(
            label = "包年包月选项",
            conditions = "this.chargeType === 'PrePaid'",
            nestedFormClass = PrepaidConfig.class
    )
    private PrepaidConfig prepaidConfig;

    @SelectField(
            label = "产品",
            options = {
                    "Community",
                    "Enterprise"
            },
            optionNames = {
                    "Redis 开源版",
                    "Tair 企业版"
            },
            defaultValues = "Community"
    )
    private String edition;

    @SelectField(
            label = "部署方式",
            options = {
                    "OnECS",
                    "Local"
            },
            optionNames = {
                    "内存型 (云原生版)",
                    "内存型 (经典版)"
            },
            defaultValues = "OnECS",
            conditions = "this.edition === 'Community'"
    )
    private String communityProductType;
    @SelectField(
            label = "部署方式",
            options = {
                    "Tair_rdb",
                    "Tair_scm",
                    "Tair_essd",
                    "Local"
            },
            optionNames = {
                    "内存型 (云原生版)",
                    "持久内存型 (云原生版)",
                    "磁盘型 (云原生版 ESSD/SSD)",
                    "内存型 (经典版)"
            },
            defaultValues = "Tair_rdb",
            conditions = "this.edition === 'Enterprise'"
    )
    private String enterpriseProductType;

    @SelectField(
            label = "系列",
            options = {
                    "professional",
                    "economical"
            },
            optionNames = {
                    "标准版",
                    "倚天版"
            },
            defaultValues = "professional",
            conditions = "this.edition === 'Community' && this.communityProductType === 'OnECS'"
    )
    private String scene;

    @SelectField(
            label = "Redis版本",
            options = {
                    "5.0"
            },
            optionNames = {
                    "5.0"
            },
            defaultValues = "5.0",
            conditions = "(this.edition === 'Community' && this.communityProductType === 'Local') || " +
                    "(this.edition === 'Enterprise' && this.enterpriseProductType === 'Local')"
    )
    private String classicEngineVersion;

    @SelectField(
            label = "Redis版本",
            options = {
                    "5.0",
                    "6.0",
                    "7.0"
            },
            optionNames = {
                    "5.0",
                    "6.0",
                    "7.0"
            },
            defaultValues = "6.0",
            conditions = "(this.edition === 'Community' && this.communityProductType === 'OnECS') || " +
                    "(this.edition === 'Enterprise' && this.enterpriseProductType === 'Tair_rdb')"
    )
    private String cloudNativeEngineVersion;

    @SelectField(
            label = "Redis版本",
            options = {
                    "6.0"
            },
            optionNames = {
                    "6.0"
            },
            defaultValues = "6.0",
            conditions = "this.edition === 'Enterprise' && (this.enterpriseProductType === 'Tair_scm' || this.enterpriseProductType === 'Tair_essd')"
    )
    private String cloudNative60EngineVersion;

    @SelectField(
            label = "架构类型",
            options = {
                    "standard",
                    "cluster"
            },
            optionNames = {
                    "标准",
                    "集群"
            },
            defaultValues = "standard"
    )
    private String architecture;

    @SelectField(
            label = "节点类型",
            options = {
                    "standalone",
                    "ha"
            },
            optionNames = {
                    "单副本",
                    "高可用"
            },
            defaultValues = "ha"
    )
    private String nodeType;

    @SelectField(
            label = "可用区类型",
            options = {
                    "single",
                    "double"
            },
            optionNames = {
                    "单可用区",
                    "双可用区",
            },
            defaultValues = "single"
    )
    private String zoneType;
    @SelectField(label = "备可用区", conditions = "this.zoneType === 'double'")
    private String slaveZone;

    @SelectField(
            label = "分片内存大小",
            options = {
                    "256",
                    "1024",
                    "2048",
                    "4096",
                    "8192",
                    "16384",
                    "24576",
                    "32768",
                    "65536"
            },
            optionNames = {
                    "256MB",
                    "1GB",
                    "2GB",
                    "4GB",
                    "8GB",
                    "16GB",
                    "24GB",
                    "32GB",
                    "64GB"
            },
            defaultValues = "1024"
    )
    private Long memoryMb;

    @NumberField(
            label = "分片数",
            min = 2,
            max = 256,
            defaultValue = 2,
            conditions = {
                    "this.architecture === 'cluster'",
                    "(this.edition === 'Community' && this.communityProductType !== 'Local') || " +
                            "(this.edition === 'Enterprise' && this.enterpriseProductType !== 'Local')"
            }
    )
    private Long cloudNativeShardNumber;
    @SelectField(
            label = "分片数",
            options = {
                    "2",
                    "4",
                    "8",
                    "16",
                    "32",
                    "64",
                    "128",
                    "256"
            },
            optionNames = {
                    "2分片",
                    "4分片",
                    "8分片",
                    "16分片",
                    "32分片",
                    "64分片",
                    "128分片",
                    "256分片"
            },
            defaultValues = "2",
            conditions = {
                    "this.architecture === 'cluster'",
                    "(this.edition === 'Community' && this.communityProductType === 'Local') || " +
                            "(this.edition === 'Enterprise' && this.enterpriseProductType === 'Local')"
            }
    )
    private Long classicShardNumber;

    @SelectField(
            label = "存储类型",
            options = {
                    "essd_pl1",
                    "essd_pl2",
                    "essd_pl3"
            },
            optionNames = {
                    "ESSD PL1",
                    "ESSD PL2",
                    "ESSD PL3"
            },
            conditions = "this.edition === 'Enterprise' && this.enterpriseProductType === 'Tair_essd'"
    )
    private String storageType;

    @NumberField(
            label = "存储空间(GB)",
            min = 60,
            conditions = "this.edition === 'Enterprise' && this.enterpriseProductType === 'Tair_essd'"
    )
    private Integer storageGb;

    @BooleanField(
            label = "读写分离",
            conditions = {
                    "this.nodeType === 'ha'",
                    "(this.edition === 'Community' && this.communityProductType !== 'Local') || " +
                            "(this.edition === 'Enterprise' && this.enterpriseProductType !== 'Local')"
            }
    )
    private boolean enableReadOnlyReplica;

    @NumberField(
            label = "主可用区节点数",
            min = 1,
            max = 9,
            conditions = {
                    "this.nodeType === 'ha'",
                    "(this.edition === 'Community' && this.communityProductType !== 'Local') || " +
                            "(this.edition === 'Enterprise' && this.enterpriseProductType !== 'Local')"
            },
            defaultValue = 2
    )
    private Long masterZoneReplicaNumber;

    @NumberField(
            label = "备可用区节点数",
            min = 1,
            max = 9,
            conditions = {
                    "this.nodeType === 'ha'",
                    "this.zoneType === 'double'",
                    "(this.edition === 'Community' && this.communityProductType !== 'Local') || " +
                            "(this.edition === 'Enterprise' && this.enterpriseProductType !== 'Local')"
            },
            defaultValue = 1
    )
    private Long slaveZoneReplicaNumber;


    @SelectField(
            label = "启用AOF",
            options = {
                    "yes", "no"
            },
            optionNames = {
                    "启用", "不启用"
            },
            defaultValues = "yes"
    )
    public String appendonly;

    @InputField(label = "实例密码", inputType = "password")
    private String password;


    @NumberField(label = "实例端口", min = 1024, max = 65535, defaultValue = 6379)
    public Integer port;

    @Data
    public static class PrepaidConfig implements DynamicForm {
        @SelectField(
                label = "购买时长",
                options = {
                        "1", "2", "3", "4", "5", "6", "7", "8", "9", "12", "24", "36", "60"
                },
                optionNames = {
                        "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月",
                        "1年", "2年", "3年", "5年"
                },
                defaultValues = "1"
        )
        public Long period;

        @BooleanField(label = "自动续费")
        public boolean autoRenew;

        @SelectField(
                label = "自动续费时长",
                options = {
                        "1", "2", "3", "6","12"
                },
                optionNames = {
                        "1个月", "2个月", "3个月", "6个月", "1年"
                },
                conditions = "this.autoRenew === true",
                defaultValues = "1"
        )
        public String autoRenewPeriod;
    }
}
