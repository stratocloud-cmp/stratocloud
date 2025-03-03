package com.stratocloud.provider.tencent.lb.backend.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentInstanceBackendBuildInput implements ResourceActionInput {
    @NumberField(label = "后端服务端口")
    private Long port;

    @NumberField(label = "转发权重")
    private Long weight;
}
