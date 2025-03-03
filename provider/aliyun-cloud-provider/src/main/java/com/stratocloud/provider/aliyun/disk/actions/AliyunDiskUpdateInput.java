package com.stratocloud.provider.aliyun.disk.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunDiskUpdateInput implements ResourceActionInput {
    @InputField(label = "云硬盘名称")
    private String diskName;


    @BooleanField(
            label = "开启性能突发"
    )
    private Boolean burstPerformance;

}
