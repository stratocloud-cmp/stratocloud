package com.stratocloud.provider.aliyun.disk.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunDiskResizeInput implements ResourceActionInput {
    @NumberField(label = "云硬盘大小")
    private Integer diskSize;
}
