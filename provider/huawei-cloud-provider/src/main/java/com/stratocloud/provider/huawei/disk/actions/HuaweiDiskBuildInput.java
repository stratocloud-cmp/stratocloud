package com.stratocloud.provider.huawei.disk.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiDiskBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费模式",
            options = {
                    "postPaid",
                    "prePaid"
            },
            optionNames = {
                    "按需",
                    "包年包月"
            },
            defaultValues = "postPaid",
            conditions = "args.isPrimaryCapability !== true"
    )
    private String chargingMode;

    @BooleanField(label = "自动续费", conditions = "this.chargingMode === 'prePaid'")
    private boolean autoRenew;

    @SelectField(
            label = "购买时长(月)",
            options = {
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
                    "7",
                    "8",
                    "9",
                    "12"
            },
            optionNames = {
                    "1个月",
                    "2个月",
                    "3个月",
                    "4个月",
                    "5个月",
                    "6个月",
                    "7个月",
                    "8个月",
                    "9个月",
                    "12个月"
            },
            defaultValues = "1",
            conditions = "this.chargingMode === 'prePaid'"
    )
    private String period;




    @NumberField(label = "磁盘大小(GB)", min = 1, defaultValue = 40)
    private Integer size;

    @SelectField(
            label = "磁盘类型",
            options = {
                    "SATA",
                    "SAS",
                    "GPSSD",
                    "SSD",
                    "ESSD",
                    "GPSSD2",
                    "ESSD2"
            },
            optionNames = {
                    "普通IO云硬盘(已售罄)",
                    "高IO云硬盘",
                    "通用型SSD云硬盘",
                    "超高IO云硬盘",
                    "极速IO云硬盘",
                    "通用型SSD V2云硬盘",
                    "极速型SSD V2云硬盘"
            },
            defaultValues = "SAS"
    )
    private String volumeType;

    @NumberField(label = "IOPS", conditions = "this.volumeType === 'GPSSD2' || this.volumeType === 'ESSD2'")
    private Integer iops;

    @NumberField(label = "吞吐量(MiB/s)", conditions = "this.volumeType === 'GPSSD2'")
    private Integer throughput;

}
