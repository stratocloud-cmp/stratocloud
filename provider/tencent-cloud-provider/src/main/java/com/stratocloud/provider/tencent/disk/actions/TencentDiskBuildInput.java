package com.stratocloud.provider.tencent.disk.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentDiskBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费方式",
            options = {
                    "POSTPAID_BY_HOUR",
                    "PREPAID",
                    "CDCPAID"
            },
            optionNames = {
                    "按小时后付费",
                    "预付费",
                    "独享集群付费"
            },
            conditions = "args.isPrimaryCapability !== true",
            defaultValues = "POSTPAID_BY_HOUR"
    )
    private String chargeType;
    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                    "1年", "2年", "3年"
            },
            conditions = "this.chargeType === 'PREPAID'"
    )
    private String prepaidPeriod;
    @SelectField(
            label = "自动续费方式",
            options = {
                    "NOTIFY_AND_AUTO_RENEW",
                    "NOTIFY_AND_MANUAL_RENEW",
                    "DISABLE_NOTIFY_AND_MANUAL_RENEW",
            },
            optionNames = {
                    "通知过期且自动续费",
                    "通知过期不自动续费",
                    "不通知过期不自动续费"
            },
            conditions = "this.chargeType === 'PREPAID'"
    )
    private String renewType;


    @SelectField(
            label = "云硬盘类型",
            options = {
                    TencentDiskType.Ids.CLOUD_PREMIUM,
                    TencentDiskType.Ids.CLOUD_SSD,
                    TencentDiskType.Ids.CLOUD_BSSD,
                    TencentDiskType.Ids.CLOUD_HSSD,
                    TencentDiskType.Ids.CLOUD_TSSD
            },
            optionNames = {
                    TencentDiskType.Names.CLOUD_PREMIUM,
                    TencentDiskType.Names.CLOUD_SSD,
                    TencentDiskType.Names.CLOUD_BSSD,
                    TencentDiskType.Names.CLOUD_HSSD,
                    TencentDiskType.Names.CLOUD_TSSD
            }
    )
    private TencentDiskType diskType;
    @NumberField(label = "云硬盘大小", defaultValue = 40)
    private Long diskSize;

    @BooleanField(label = "高级选项")
    private Boolean enableAdvanceOptions;

    @NumberField(
            label = "购买额外性能(MBps)",
            conditions = {
                    "this.enableAdvanceOptions === true",
                    "this.diskType === 'CLOUD_TSSD' || this.diskType === 'CLOUD_HSSD'"
            }
    )
    private Long throughputPerformance;
    @BooleanField(
            label = "加密云硬盘",
            conditions = "this.enableAdvanceOptions === true",
            description = "作为系统盘时将忽略此参数"
    )
    private Boolean encrypt;
    @NumberField(
            label = "备份点配额",
            conditions = "this.enableAdvanceOptions === true"
    )
    private Long backupQuota;
    @NumberField(
            label = "开启性能突发",
            conditions = "this.enableAdvanceOptions === true"
    )
    private Boolean burstPerformance;


}
