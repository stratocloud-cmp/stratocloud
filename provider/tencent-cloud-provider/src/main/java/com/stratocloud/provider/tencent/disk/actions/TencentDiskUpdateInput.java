package com.stratocloud.provider.tencent.disk.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentDiskUpdateInput implements ResourceActionInput {
    @InputField(label = "云硬盘名称")
    private String diskName;

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

    @NumberField(
            label = "开启性能突发"
    )
    private Boolean burstPerformance;


}
