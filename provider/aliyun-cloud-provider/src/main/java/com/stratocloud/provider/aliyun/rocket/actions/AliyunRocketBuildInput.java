package com.stratocloud.provider.aliyun.rocket.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunRocketBuildInput implements ResourceActionInput {
    @SelectField(
            label = "付费类型",
            options = {
                    "PayAsYouGo",
                    "Subscription"
            },
            optionNames = {
                    "按量付费",
                    "包年包月"
            },
            defaultValues = "PayAsYouGo"
    )
    private String paymentType;

    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "12", "24", "36"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "1年", "2年", "3年"
            },
            defaultValues = "1",
            conditions = "this.paymentType === 'Subscription'"
    )
    private Long period;

    @BooleanField(label = "自动续费")
    private boolean autoRenew;

    @SelectField(
            label = "自动续费时长",
            options = {
                    "1", "2", "3", "6", "12"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "6个月", "1年"
            },
            defaultValues = "1",
            conditions = "this.paymentType === 'Subscription'"
    )
    private Long autoRenewPeriod;

    @SelectField(
            label = "实例主系列",
            options = {
                    "standard",
                    "professional",
                    "ultimate"
            },
            optionNames = {
                    "标准版",
                    "专业版",
                    "铂金版"
            },
            defaultValues = "standard"
    )
    private String seriesCode;

    @SelectField(
            label = "实例子系列",
            options = {
                    "cluster_ha"
            },
            optionNames = {
                    "集群高可用版"
            },
            defaultValues = "cluster_ha"
    )
    private String subSeriesCode;

    @SelectField(
            label = "实例规格",
            options = {
                    "rmq.s2.2xlarge",
                    "rmq.s2.4xlarge",
                    "rmq.s2.6xlarge"
            },
            optionNames = {
                    "TPS上限:2000 连接数上限:4000 Topic免费配额:100 Topic最大配额:300 消费者组最大配额:1000",
                    "TPS上限:4000 连接数上限:4000 Topic免费配额:100 Topic最大配额:300 消费者组最大配额:1000",
                    "TPS上限:6000 连接数上限:6000 Topic免费配额:100 Topic最大配额:500 消费者组最大配额:1000"
            },
            conditions = "this.seriesCode === 'standard' && this.subSeriesCode === 'cluster_ha'",
            defaultValues = "rmq.s2.2xlarge"
    )
    private String standardMsgProcessSpec;

    @SelectField(
            label = "实例规格",
            options = {
                    "rmq.p2.2xlarge",
                    "rmq.p2.4xlarge",
                    "rmq.p2.6xlarge",
                    "rmq.p2.10xlarge",
                    "rmq.p2.20xlarge",
                    "rmq.p2.30xlarge",
                    "rmq.p2.40xlarge",
                    "rmq.p2.50xlarge",
                    "rmq.p2.100xlarge",
                    "rmq.p2.120xlarge",
                    "rmq.p2.150xlarge",
                    "rmq.p2.200xlarge"
            },
            optionNames = {
                    "TPS上限:2000 突发TPS上限:1000 连接数上限:4000 Topic免费配额:150 Topic最大配额:500 消费者组最大配额:2000",
                    "TPS上限:4000 突发TPS上限:2000 连接数上限:4000 Topic免费配额:150 Topic最大配额:500 消费者组最大配额:2000",
                    "TPS上限:6000 突发TPS上限:3000 连接数上限:6000 Topic免费配额:150 Topic最大配额:500 消费者组最大配额:2000",
                    "TPS上限:10000 突发TPS上限:5000 连接数上限:10000 Topic免费配额:150 Topic最大配额:1000 消费者组最大配额:2000",
                    "TPS上限:20000 突发TPS上限:10000 连接数上限:10000 Topic免费配额:150 Topic最大配额:1000 消费者组最大配额:2000",
                    "TPS上限:30000 突发TPS上限:15000 连接数上限:12000 Topic免费配额:150 Topic最大配额:2000 消费者组最大配额:2000",
                    "TPS上限:40000 突发TPS上限:20000 连接数上限:12000 Topic免费配额:150 Topic最大配额:2000 消费者组最大配额:2000",
                    "TPS上限:50000 突发TPS上限:20000 连接数上限:14000 Topic免费配额:150 Topic最大配额:2000 消费者组最大配额:2000",
                    "TPS上限:100000 突发TPS上限:30000 连接数上限:26000 Topic免费配额:150 Topic最大配额:2000 消费者组最大配额:2000",
                    "TPS上限:120000 突发TPS上限:40000 连接数上限:30000 Topic免费配额:150 Topic最大配额:2000 消费者组最大配额:2000",
                    "TPS上限:150000 突发TPS上限:50000 连接数上限:38000 Topic免费配额:150 Topic最大配额:2000 消费者组最大配额:2000",
                    "TPS上限:200000 突发TPS上限:60000 连接数上限:50000 Topic免费配额:150 Topic最大配额:2000 消费者组最大配额:2000"
            },
            conditions = "this.seriesCode === 'professional' && this.subSeriesCode === 'cluster_ha'",
            defaultValues = "rmq.p2.2xlarge"
    )
    private String professionalMsgProcessSpec;


    @SelectField(
            label = "实例规格",
            options = {
                    "rmq.u2.10xlarge",
                    "rmq.u2.20xlarge",
                    "rmq.u2.30xlarge",
                    "rmq.u2.40xlarge",
                    "rmq.u2.50xlarge",
                    "rmq.u2.60xlarge",
                    "rmq.u2.70xlarge",
                    "rmq.u2.80xlarge",
                    "rmq.u2.90xlarge",
                    "rmq.u2.100xlarge",
                    "rmq.u2.120xlarge",
                    "rmq.u2.150xlarge",
                    "rmq.u2.200xlarge",
                    "rmq.u2.250xlarge",
                    "rmq.u2.300xlarge",
                    "rmq.u2.350xlarge",
                    "rmq.u2.400xlarge",
                    "rmq.u2.450xlarge",
                    "rmq.u2.500xlarge",
                    "rmq.u2.550xlarge",
                    "rmq.u2.600xlarge",
                    "rmq.u2.1000xlarge"
            },
            optionNames = {
                    "TPS上限:10000 突发TPS上限:5000 连接数上限:10000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:20000 突发TPS上限:10000 连接数上限:10000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:30000 突发TPS上限:15000 连接数上限:12000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:40000 突发TPS上限:20000 连接数上限:12000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:50000 突发TPS上限:20000 连接数上限:14000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:60000 突发TPS上限:22000 连接数上限:16000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:70000 突发TPS上限:24000 连接数上限:18000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:80000 突发TPS上限:26000 连接数上限:20000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:90000 突发TPS上限:28000 连接数上限:24000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:100000 突发TPS上限:30000 连接数上限:26000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:120000 突发TPS上限:40000 连接数上限:30000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:150000 突发TPS上限:50000 连接数上限:38000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:200000 突发TPS上限:60000 连接数上限:50000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:250000 突发TPS上限:70000 连接数上限:51000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:300000 突发TPS上限:80000 连接数上限:52000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:350000 突发TPS上限:90000 连接数上限:53000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:400000 突发TPS上限:100000 连接数上限:54000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:450000 突发TPS上限:120000 连接数上限:60000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:500000 突发TPS上限:140000 连接数上限:66000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:550000 突发TPS上限:160000 连接数上限:72000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:600000 突发TPS上限:200000 连接数上限:80000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000",
                    "TPS上限:1000000 突发TPS上限:300000 连接数上限:134000 Topic免费配额:200 Topic最大配额:3000 消费者组最大配额:4000"
            },
            conditions = "this.seriesCode === 'ultimate' && this.subSeriesCode === 'cluster_ha'",
            defaultValues = "rmq.u2.10xlarge"
    )
    private String ultimateMsgProcessSpec;

    @InputField(label = "消息发送TPS占整个实例消息收发TPS总量的比例", defaultValue = "0.5", regex = "^(0\\.\\d+|1\\.0*)$", regexMessage = "请输入0-1之间的小数")
    private Float sendReceiveRatio;

    @BooleanField(label = "开启规格外突发弹性能力", conditions = "this.seriesCode === 'professional' || this.seriesCode === 'ultimate'")
    private boolean autoScaling;

    @NumberField(label = "消息保留时长(小时)", defaultValue = 72, min = 1)
    private Integer messageRetentionTime;

    @BooleanField(label = "开启云盘加密", conditions = "this.seriesCode === 'ultimate'")
    private boolean storageEncryption;

    @InputField(label = "云盘密钥", conditions = "this.storageEncryption === true && this.seriesCode === 'ultimate'")
    private String storageSecretKey;

    @SelectField(
            label = "公网访问",
            options = {
                    "enable",
                    "disable"
            },
            optionNames = {
                    "开通",
                    "关闭"
            },
            defaultValues = "disable"
    )
    private String internetSpec;

    @SelectField(
            label = "公网计费类型",
            options = {
                    "payByBandwidth",
                    "payByTraffic"
            },
            optionNames = {
                    "固定带宽计费",
                    "按流量计费"
            },
            defaultValues = "payByBandwidth",
            conditions = "this.internetSpec === 'enable'"
    )
    private String flowOutType;

    @NumberField(
            label = "公网带宽(Mb/s)",
            conditions = "this.internetSpec === 'enable' && this.flowOutType === 'payByBandwidth'",
            min = 1,
            max = 1000
    )
    private Integer flowOutBandwidth;
}
