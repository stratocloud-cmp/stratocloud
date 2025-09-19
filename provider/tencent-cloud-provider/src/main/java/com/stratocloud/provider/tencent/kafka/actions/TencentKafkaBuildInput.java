package com.stratocloud.provider.tencent.kafka.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class TencentKafkaBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费模式",
            options = {
                    "PREPAID",
                    "POSTPAID"
            },
            optionNames = {
                    "包年包月",
                    "按小时后付费"
            },
            defaultValues = "POSTPAID"
    )
    private String payType;

    @SelectField(
            label = "购买时长",
            options = {
                    "1m", "2m", "3m", "4m", "5m", "6m", "7m", "8m", "9m", "10m", "11m", "12m", "24m", "36m"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                    "1年", "2年", "3年"
            },
            conditions = "this.payType === 'PREPAID'",
            defaultValues = "1m"
    )
    private String period;

    @SelectField(
            label = "是否自动续费",
            options = {
                    "2",
                    "1"
            },
            optionNames = {
                    "手动续费",
                    "自动续费"
            },
            defaultValues = "2",
            conditions = "this.payType === 'PREPAID'"
    )
    private Long autoRenewFlag;

    /**
     * 实例版本。目前支持 "0.10.2","1.1.1","2.4.1","2.4.2","2.8.1"。"2.4.1" 与 "2.4.2" 属于同一个版本，传任意一个均可。
     */
    @SelectField(
            label = "Kafka版本",
            options = {
                    "2.4.2",
                    "2.8.1",
                    "3.2.3"
            },
            optionNames = {
                    "2.4.2",
                    "2.8.1",
                    "3.2.3"
            },
            defaultValues = "2.8.1"
    )
    private String kafkaVersion;

    @SelectField(
            label = "实例类型",
            options = {
                    "SMALL",
                    "BIG"
            },
            optionNames = {
                    "20 ~ 1200MB/s",
                    "1600MB/s以上"
            },
            defaultValues = "SMALL"
    )
    private BandwidthType bandwidthType;
    /**
     * 实例内网峰值带宽。单位 MB/s。标准版需传入当前实例规格所对应的峰值带宽。注意如果创建的实例为专业版实例，峰值带宽，分区数等参数配置需要满足专业版的计费规格。
     */
    @NumberField(
            label = "内网峰值带宽(MB/s)",
            min = 20,
            max = 1200,
            step = 20,
            defaultValue = 20,
            conditions = "this.bandwidthType === 'SMALL'"
    )
    private Long bandwidthSmall;

    @NumberField(
            label = "内网峰值带宽(MB/s)",
            min = 1600,
            max = 100000,
            step = 200,
            defaultValue = 1600,
            conditions = "this.bandwidthType === 'BIG'"
    )
    private Long bandwidthBig;

    /**
     * 专业版实例磁盘类型，标准版实例不需要填写。"CLOUD_SSD"：SSD云硬盘；"CLOUD_BASIC"：高性能云硬盘。不传默认值为 "CLOUD_BASIC"
     */
    @SelectField(
            label = "磁盘类型",
            options = {
                    "CLOUD_BASIC",
                    "CLOUD_SSD"
            },
            optionNames = {
                    "高性能云硬盘",
                    "SSD云硬盘"
            },
            defaultValues = "CLOUD_BASIC"
    )
    private String diskType;

    /**
     * 实例硬盘大小，需要满足当前实例的计费规格
     */
    @NumberField(
            label = "硬盘大小(GB)",
            min = 200,
            max = 500000,
            step = 100,
            defaultValue = 200,
            conditions = "this.bandwidthType === 'SMALL'"
    )
    private Long diskSizeSmall;
    @NumberField(
            label = "硬盘大小(GB)",
            min = 10000,
            max = 500000,
            step = 100,
            defaultValue = 10000,
            conditions = "this.bandwidthType === 'BIG'"
    )
    private Long diskSizeBig;


    /**
     * 实例日志的默认最长保留时间，单位分钟。不传入该参数时默认为 1440 分钟（1天），最大30天。当 topic 显式设置消息保留时间时，以 topic 保留时间为准
     */
    @NumberField(label = "日志默认最长保留时间(小时)", defaultValue = 72, min = 24, max = 2160)
    private Long msgRetentionTime;

    /**
     * 实例最大分区数量，需要满足当前实例的计费规格
     */
    @NumberField(
            label = "最大分区数量",
            min = 400,
            max = 40000,
            step = 100,
            defaultValue = 400
    )
    private Long partition;

    /**
     * 实例最大 topic 数量，需要满足当前实例的计费规格
     */
    @NumberField(
            label = "最大topic数量",
            min = 200,
            step = 100,
            defaultValue = 200,
            conditions = "this.payType === 'POSTPAID'"
    )
    private Long topicNum;

    @BooleanField(label = "跨AZ部署")
    private boolean multiZone;

    @SelectField(label = "多可用区", multiSelect = true, conditions = "this.multiZone === true")
    private List<Long> zoneIds;


    /**
     * 公网带宽大小，单位 Mbps。默认是没有加上免费 3Mbps 带宽。例如总共需要 3Mbps 公网带宽，此处传 0；总共需要 6Mbps 公网带宽，此处传 3。需要保证传入参数为 3 的整数倍
     */
    @NumberField(
            label = "公网带宽(Mbps)",
            min = 3,
            max = 999,
            step = 3,
            defaultValue = 3
    )
    private Long publicNetworkMonthly;


    /**
     * 弹性带宽开关 0不开启  1开启（0默认)
     */
    @SelectField(
            label = "弹性带宽",
            options = {
                    "0",
                    "1"
            },
            optionNames = {
                    "不开启",
                    "开启"
            },
            defaultValues = "0"
    )
    private Long elasticBandwidthSwitch;

    public enum BandwidthType {
        SMALL, BIG
    }
}
