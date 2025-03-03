package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.requirements;

import com.stratocloud.form.NumberField;
import com.stratocloud.provider.relationship.RelationshipConnectInput;
import lombok.Data;

@Data
public class AliyunClbServerGroupConnectInput implements RelationshipConnectInput {
    @NumberField(label = "后端服务权重", min = 1, max = 100)
    private Integer weight;
    @NumberField(label = "后端服务端口", min = 1, max = 65535)
    private Integer port;
}
