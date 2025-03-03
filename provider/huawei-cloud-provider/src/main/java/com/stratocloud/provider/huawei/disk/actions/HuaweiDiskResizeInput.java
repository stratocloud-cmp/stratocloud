package com.stratocloud.provider.huawei.disk.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiDiskResizeInput implements ResourceActionInput {
    @NumberField(label = "云硬盘大小")
    private Integer diskSize;
}
