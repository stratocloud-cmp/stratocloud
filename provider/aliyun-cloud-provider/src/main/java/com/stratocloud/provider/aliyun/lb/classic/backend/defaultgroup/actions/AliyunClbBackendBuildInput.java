package com.stratocloud.provider.aliyun.lb.classic.backend.defaultgroup.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunClbBackendBuildInput implements ResourceActionInput {
    @NumberField(label = "后端服务权重", min = 1, max = 100, defaultValue = 100)
    private Integer weight;
    @NumberField(label = "后端服务端口", min = 1, max = 65535, defaultValue = 80)
    private Integer port;
}
