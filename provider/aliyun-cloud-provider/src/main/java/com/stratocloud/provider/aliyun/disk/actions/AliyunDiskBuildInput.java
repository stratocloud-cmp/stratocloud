package com.stratocloud.provider.aliyun.disk.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.disk.AliyunDiskCategory;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunDiskBuildInput implements ResourceActionInput {
    @SelectField(
            label = "云硬盘类型",
            options = {
                    AliyunDiskCategory.Ids.CLOUD,
                    AliyunDiskCategory.Ids.CLOUD_EFFICIENCY,
                    AliyunDiskCategory.Ids.CLOUD_SSD,
                    AliyunDiskCategory.Ids.CLOUD_ESSD,
                    AliyunDiskCategory.Ids.CLOUD_AUTO,
                    AliyunDiskCategory.Ids.CLOUD_ESSD_ENTRY,
                    AliyunDiskCategory.Ids.ELASTIC_EPHEMERAL_DISK_STANDARD,
                    AliyunDiskCategory.Ids.ELASTIC_EPHEMERAL_DISK_PREMIUM,
            },
            optionNames = {
                    AliyunDiskCategory.Names.CLOUD,
                    AliyunDiskCategory.Names.CLOUD_EFFICIENCY,
                    AliyunDiskCategory.Names.CLOUD_SSD,
                    AliyunDiskCategory.Names.CLOUD_ESSD,
                    AliyunDiskCategory.Names.CLOUD_AUTO,
                    AliyunDiskCategory.Names.CLOUD_ESSD_ENTRY,
                    AliyunDiskCategory.Names.ELASTIC_EPHEMERAL_DISK_STANDARD,
                    AliyunDiskCategory.Names.ELASTIC_EPHEMERAL_DISK_PREMIUM,
            },
            defaultValues = "cloud_efficiency"
    )
    private AliyunDiskCategory diskCategory;
    @NumberField(label = "云硬盘大小", defaultValue = 40)
    private Integer diskSize;

    @BooleanField(label = "高级选项")
    private Boolean enableAdvanceOptions;

    @BooleanField(
            label = "加密云硬盘",
            conditions = "this.enableAdvanceOptions === true"
    )
    private Boolean encrypted;
    @NumberField(
            label = "开启性能突发",
            conditions = "this.enableAdvanceOptions === true"
    )
    private Boolean burstPerformance;

    @SelectField(
            label = "性能等级",
            conditions = "this.enableAdvanceOptions === true && this.diskCategory === 'cloud_essd'",
            options = {
                    "PL0",
                    "PL1",
                    "PL2",
                    "PL3"
            },
            optionNames = {
                    "PL0: 单盘最高随机读写 IOPS 1 万",
                    "PL1: 单盘最高随机读写 IOPS 5 万",
                    "PL2: 单盘最高随机读写 IOPS 10 万",
                    "PL3: 单盘最高随机读写 IOPS 100 万"
            },
            defaultValues = "PL1"

    )
    private String performanceLevel;
}
