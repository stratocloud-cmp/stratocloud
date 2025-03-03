package com.stratocloud.provider.huawei.disk.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiDiskUpdateInput implements ResourceActionInput {
    @InputField(label = "云硬盘名称")
    private String diskName;
}
