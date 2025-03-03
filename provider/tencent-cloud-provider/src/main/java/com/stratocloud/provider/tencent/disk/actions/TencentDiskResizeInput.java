package com.stratocloud.provider.tencent.disk.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentDiskResizeInput implements ResourceActionInput {
    @NumberField(label = "云硬盘大小")
    private Long diskSize;
}
